package org.sa.rainbow.stitch.adaptation;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.acmestudio.acme.element.IAcmeSystem;
import org.apache.commons.lang.time.StopWatch;
import org.sa.rainbow.core.AbstractRainbowRunnable;
import org.sa.rainbow.core.Rainbow;
import org.sa.rainbow.core.RainbowComponentT;
import org.sa.rainbow.core.RainbowConstants;
import org.sa.rainbow.core.adaptation.IAdaptationExecutor;
import org.sa.rainbow.core.adaptation.IAdaptationManager;
import org.sa.rainbow.core.error.RainbowConnectionException;
import org.sa.rainbow.core.event.IRainbowMessage;
import org.sa.rainbow.core.health.IRainbowHealthProtocol;
import org.sa.rainbow.core.models.IModelInstance;
import org.sa.rainbow.core.models.UtilityPreferenceDescription;
import org.sa.rainbow.core.models.UtilityPreferenceDescription.UtilityAttributes;
import org.sa.rainbow.core.ports.IModelChangeBusPort;
import org.sa.rainbow.core.ports.IModelChangeBusSubscriberPort;
import org.sa.rainbow.core.ports.IModelChangeBusSubscriberPort.IRainbowChangeBusSubscription;
import org.sa.rainbow.core.ports.IModelChangeBusSubscriberPort.IRainbowModelChangeCallback;
import org.sa.rainbow.core.ports.IRainbowAdaptationEnqueuePort;
import org.sa.rainbow.core.ports.IRainbowReportingPort;
import org.sa.rainbow.core.ports.RainbowPortFactory;
import org.sa.rainbow.model.acme.AcmeModelInstance;
import org.sa.rainbow.model.acme.AcmeRainbowCommandEvent.CommandEventT;
import org.sa.rainbow.stitch.Ohana;
import org.sa.rainbow.stitch.core.Strategy;
import org.sa.rainbow.stitch.core.Tactic;
import org.sa.rainbow.stitch.core.UtilityFunction;
import org.sa.rainbow.stitch.error.DummyStitchProblemHandler;
import org.sa.rainbow.stitch.error.StitchProblem;
import org.sa.rainbow.stitch.visitor.Stitch;
import org.sa.rainbow.util.Beacon;
import org.sa.rainbow.util.Util;

/**
 * The Rainbow Adaptation Engine... Currently implements a learner interface to interact with Nick Lynn's learner.
 * 
 * @author Shang-Wen Cheng (zensoul@cs.cmu.edu)
 */
public final class AdaptationManager extends AbstractRainbowRunnable implements IAdaptationManager<Strategy>/*
 * implements
 * IRainbowLearner
 */, IRainbowModelChangeCallback {


    public enum Mode {
        SERIAL, MULTI_PRONE
    };

    public static final String                      NAME                       = "Rainbow Adaptation Manager";
    public static final double                      FAILURE_RATE_THRESHOLD     = 0.95;
    public static final double                      MIN_UTILITY_THRESHOLD      = 0.40;
    private static double                           m_minUtilityThreshold      = 0.0;
    public static final long                        FAILURE_EFFECTIVE_WINDOW   = 2000 /* ms */;
    public static final long                        FAILURE_WINDOW_CHUNK       = 1000 /* ms */;
    private static final int                        SLEEP_TIME                 = 10000;
    /**
     * The prefix to be used by the strategy which takes a "leap" by achieving a similar adaptation that would have
     * taken multiple increments of the non-leap version, but at a potential "cost" in non-dire scenarios.
     */
    public static final String                      LEAP_STRATEGY_PREFIX       = "Leap-";
    /**
     * The prefix to represent the corresponding multi-step strategy of the leap-version strategy.
     */
    public static final String                      MULTI_STRATEGY_PREFIX      = "Multi-";

    private Mode                                    m_mode                     = Mode.SERIAL;
    private AcmeModelInstance                       m_model                    = null;
    private boolean                                 m_adaptNeeded              = false;                       // treat as synonymous with
    // constraint being violated
    private boolean                                 m_adaptEnabled             = true;                        // by default, we adapt
    private List<Stitch>                            m_repertoire               = null;
    private SortedMap<String, UtilityFunction>      m_utils                    = null;
    private List<Strategy>                          m_pendingStrategies        = null;

    // track history
    private String                                  m_historyTrackUtilName     = null;
    private Map<String, int[]>                      m_historyCnt               = null;
    private Map<String, Beacon>                     m_failTimer                = null;
    private IRainbowAdaptationEnqueuePort<Strategy> m_enqueuePort              = null;
    private IModelChangeBusSubscriberPort           m_modelChangePort          = null;
    private String                                  m_modelRef;
    private IRainbowChangeBusSubscription           m_modelTypecheckingChanged = new IRainbowChangeBusSubscription () {

        @Override
        public
        boolean
        matches (IRainbowMessage message) {
            String type = (String )message
                    .getProperty (IModelChangeBusPort.EVENT_TYPE_PROP);
            String modelName = (String )message
                    .getProperty (IModelChangeBusPort.MODEL_NAME_PROP);
            String modelType = (String )message
                    .getProperty (IModelChangeBusPort.MODEL_TYPE_PROP);
            try {
                CommandEventT ct = CommandEventT
                        .valueOf (type);
                return (ct.isEnd ()
                        && "setTypecheckResult"
                        .equals (message
                                .getProperty (IModelChangeBusPort.COMMAND_PROP)) && m_modelRef
                                .equals (Util
                                        .genModelRef (
                                                modelName,
                                                modelType)));
            }
            catch (Exception e) {
                return false;
            }
        }
    };

    /**
     * Default constructor.
     */
    public AdaptationManager () {
        super (NAME);

        m_repertoire = new ArrayList<Stitch> ();
        m_utils = new TreeMap<String, UtilityFunction> ();
        m_pendingStrategies = new ArrayList<Strategy> ();
        m_historyTrackUtilName = Rainbow.getProperty (RainbowConstants.PROPKEY_TRACK_STRATEGY);
        if (m_historyTrackUtilName != null) {
            m_historyCnt = new HashMap<String, int[]> ();
            m_failTimer = new HashMap<String, Beacon> ();
        }
        String thresholdStr = Rainbow.getProperty (RainbowConstants.PROPKEY_UTILITY_MINSCORE_THRESHOLD);
        if (thresholdStr == null) {
            m_minUtilityThreshold = MIN_UTILITY_THRESHOLD;
        }
        else {
            m_minUtilityThreshold = Double.valueOf (thresholdStr);
        }
        setSleepTime (SLEEP_TIME);

    }
    @Override
    public void initialize (IRainbowReportingPort port) throws RainbowConnectionException {
        super.initialize (port);
        initConnectors ();
        UtilityPreferenceDescription preferenceDesc = Rainbow.instance ().getRainbowMaster ().preferenceDesc ();
        for (String k : preferenceDesc.utilities.keySet ()) {
            UtilityAttributes ua = preferenceDesc.utilities.get (k);
            UtilityFunction uf = new UtilityFunction (k, ua.label, ua.mapping, ua.desc, ua.values);
            m_utils.put (k, uf);
        }
        initAdaptationRepertoire ();

    }

    private void initConnectors () throws RainbowConnectionException {
        m_modelChangePort = RainbowPortFactory.createModelChangeBusSubscriptionPort ();
        m_modelChangePort.subscribe (m_modelTypecheckingChanged, this);
    }

    @Override
    public void setModelToManage (String modelName, String modelType) {
        m_modelRef = modelName + ":" + modelType;
        m_model = (AcmeModelInstance )Rainbow.instance ().getRainbowMaster ().modelsManager ()
                .<IAcmeSystem> getModelInstance (modelType, modelName);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.sa.rainbow.core.IDisposable#dispose()
     */
    @Override
    public void dispose () {
        for (Stitch stitch : m_repertoire) {
            stitch.dispose ();
        }
        m_repertoire.clear ();
        m_utils.clear ();
        m_pendingStrategies.clear ();
        if (m_historyTrackUtilName != null) {
            m_historyCnt.clear ();
            m_failTimer.clear ();
            m_historyCnt = null;
            m_failTimer = null;
        }

        // null-out data members
        m_repertoire = null;
        m_utils = null;
        m_pendingStrategies = null;
        m_historyTrackUtilName = null;
        m_model = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.sa.rainbow.core.AbstractRainbowRunnable#log(java.lang.String)
     */
    @Override
    protected void log (String txt) {
        m_reportingPort.info (RainbowComponentT.ADAPTATION_MANAGER, txt);
    }

    public boolean adaptationEnabled () {
        return m_adaptEnabled;
    }

    public void setAdaptationEnabled (boolean b) {
        m_adaptEnabled = b;
    }

    public boolean adaptationInProgress () {
        return m_adaptNeeded;
    }

    /**
     * Removes a Strategy from the list of pending strategies, marking it as being completed (doesn't incorporate
     * outcome).
     * 
     * @param strategy
     *            the strategy to mark as being executed.
     */
    @Override
    public void markStrategyExecuted (Strategy strategy) {
        if (m_pendingStrategies.contains (strategy)) {
            m_pendingStrategies.remove (strategy);
            String s = strategy.getName () + ";" + strategy.outcome ();
            log ("*S* outcome: " + s);
            Util.dataLogger ().info (IRainbowHealthProtocol.DATA_ADAPTATION_STRATEGY + s);
            tallyStrategyOutcome (strategy);
        }
        if (m_pendingStrategies.size () == 0) {
            Util.dataLogger ().info (IRainbowHealthProtocol.DATA_ADAPTATION_END);
            // reset adaptation flags
            m_adaptNeeded = false;
//            m_model.clearConstraintViolated ();
        }
    }

    /**
     * Computes instantaneous utility of target system given current conditions.
     * 
     * @return double the instantaneous utility of current conditions
     */
    public double computeSystemInstantUtility () {
        Map<String, Double> weights = Rainbow.instance ().getRainbowMaster ().preferenceDesc ().weights.get (Rainbow
                .getProperty (RainbowConstants.PROPKEY_SCENARIO));
        double[] conds = new double[m_utils.size ()];
        int i = 0;
        double score = 0.0;
        for (String k : new ArrayList<String> (m_utils.keySet ())) {
            double v = 0.0;
            // find the applicable utility function
            UtilityFunction u = m_utils.get (k);
            // add attribute value from current condition to accumulated agg
            // value
            Object condVal = m_model.getProperty (u.mapping ());
            if (condVal != null && condVal instanceof Double) {
                m_reportingPort.trace (getComponentType (), "Avg value of prop: " + u.mapping () + " == " + condVal);
                conds[i] = ((Double )condVal).doubleValue ();
                v += conds[i];
            }
            // now compute the utility, apply weight, and accumulate to sum
            if (weights.containsKey (k)) { // but only if weight is defined
                score += weights.get (k) * u.f (v);
            }
        }
        return score;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.sa.rainbow.core.AbstractRainbowRunnable#runAction()
     */
    @Override
    protected void runAction () {
        if (m_adaptEnabled) {
//            RainbowModelTypecheckExtension ext = (RainbowModelTypecheckExtension )m_model.getModelInstance ()
//                    .getUserData ("TYPECHECKS");
//            if (ext == null || ext.typechecks ()) return;
            if (m_mode == Mode.SERIAL && m_pendingStrategies.size () > 0)
            // Only go if there are no pending strategies
                return;
            Util.dataLogger ().info (IRainbowHealthProtocol.DATA_ADAPTATION_SELECTION_BEGIN);
            Strategy selectedStrategy = checkAdaptation ();
            Util.dataLogger ().info (IRainbowHealthProtocol.DATA_ADAPTATION_SELECTION_END);
            if (selectedStrategy != null) {
                log (">> do strategy: " + selectedStrategy.getName ());
                // strategy args removed...
                Object[] args = new Object[0];
                m_pendingStrategies.add (selectedStrategy);
//                m_enqueuePort.offer (selectedStrategy, args);
                IAdaptationExecutor<Strategy> executor = Rainbow.instance ().getRainbowMaster ()
                        .<Strategy> strategyExecutor (m_modelRef);
                if (executor != null) {
                    executor.enqueueStrategy (selectedStrategy, args);
                    log ("<< Adaptation cycle awaits Executor...");
                }
            }

        }

//        if (m_adaptEnabled && m_adaptNeeded) {
//            if ((m_mode == Mode.SERIAL && m_pendingStrategies.size () == 0) || m_mode == Mode.MULTI_PRONE) {
//                // in serial mode, only do adaptation if no strategy is pending
//                // in multi-prone mode, just do adaptation
//                Util.dataLogger ().info (IRainbowHealthProtocol.DATA_ADAPTATION_BEGIN);
//                doAdaptation ();
//            }
//        }
    }

    /**
     * For JUnit testing, used to set a stopwatch object used to time duration.
     */
    StopWatch _stopWatchForTesting = null;

    /**
     * For JUnit testing, allows fetching the strategy repertoire. NOT for public use!
     * 
     * @return list of Stitch objects loaded at initialization from stitch file.
     */
    List<Stitch> _retrieveRepertoireForTesting () {
        return m_repertoire;
    }

    /**
     * For JUnit testing, allows fetching the utility objects. NOT for public use!
     * 
     * @return map of utility identifiers to functions.
     */
    Map<String, UtilityFunction> _retrieveUtilityProfilesForTesting () {
        return m_utils;
    }

    /**
     * For JUnit testing, allows re-invoking defineAttributes to artificially increase the number of quality dimensions
     * in tactic attribute vectors.
     * 
     * @param stitch
     * @param attrVectorMap
     */
    void _defineAttributesFromTester (Stitch stitch, Map<String, Map<String, Object>> attrVectorMap) {
        defineAttributes (stitch, attrVectorMap);
    }

    /*
     * Algorithm: - Iterate through repertoire searching for enabled strategies,
     * where "enabled" means applicable to current system condition NOTE: A
     * Strategy is "applicable" iff the conditions of applicability of the root
     * tactic is true TODO: Need to check if above is good assumption -
     * Calculate scores of the enabled strategies = this involves evaluating the
     * meta-information of the tactics in each strategy - Select and execute the
     * highest scoring strategy
     */
    private Strategy checkAdaptation () {
        log ("Checking if adaptation is required.");
        if (_stopWatchForTesting != null) {
            _stopWatchForTesting.start ();
        }

        int availCnt = 0;
        Map<String, Strategy> appSubsetByName = new HashMap<String, Strategy> ();
        for (Stitch stitch : m_repertoire) {
            if (!stitch.script.isApplicableForSystem (m_model)) {
                m_reportingPort.trace (getComponentType (), "x. skipping " + stitch.script.getName ());
                continue; // skip checking this script
            }
            for (Strategy strategy : stitch.script.strategies) {
                ++availCnt;
                // check first for prior failures
                if (getFailureRate (strategy) > FAILURE_RATE_THRESHOLD) {
                    continue; // don't consider this Strategy
                }
                // get estimated time cost for predicted property
                long dur = 0L;
//                if (Rainbow.predictionEnabled ()) { // provide future duration
//                    dur = strategy.estimateAvgTimeCost ();
//                }
                Map<String, Object> moreVars = new HashMap<String, Object> ();
                moreVars.put ("_dur_", dur);
                // check condition of Strategy applicability
                if (strategy.isApplicable (moreVars)) {
                    appSubsetByName.put (strategy.getName (), strategy);
                }
            }
        }
        if (appSubsetByName.size () == 0) { // can't do adaptation
            log ("No applicable Strategies to do an adaptation!");
            m_adaptNeeded = false;
//            m_model.clearConstraintViolated ();
            return null;
        }

        // check for leap-version strategy to see whether to "chain" util
        // computation 
        for (String name : appSubsetByName.keySet ().toArray (new String[0])) {
            Strategy strategy = appSubsetByName.get (name);
            Strategy leap = appSubsetByName.get (LEAP_STRATEGY_PREFIX + name);
            if (leap != null) { // Leap-version exists
                /*
                 * To chain: Determine the integer multiple N of Leap over this,
                 * then compute aggregate attributes using previous attributes
                 * as the starting point, repeating N-1 times.
                 */
                // HACK: use the first argument of the tactic closest to root
                int factor = 1;
                double stratArgVal = strategy.getFirstTacticArgumentValue ();
                double leapArgVal = leap.getFirstTacticArgumentValue ();
                if (stratArgVal != Double.NaN && leapArgVal != Double.NaN) {
                    // compute multiple now
                    factor = (int )(leapArgVal / stratArgVal);
                }
                Strategy multi = strategy.clone ();
                multi.setName (MULTI_STRATEGY_PREFIX + strategy.getName ());
                multi.multiples = factor;
                appSubsetByName.put (multi.getName (), multi);
                ++availCnt;
            }
        }
        log (">> repertoire: " + appSubsetByName.size () + " / " + availCnt + " strategy"
                + (availCnt > 1 ? "ies" : "y"));
        SortedMap<Double, Strategy> scoredStrategies = scoreStrategies (appSubsetByName);
        if (Util.dataLogger ().isInfoEnabled ()) {
            StringBuffer buf = new StringBuffer ();
            buf.append ("  [\n");
            for (Map.Entry<Double, Strategy> entry : scoredStrategies.entrySet ()) {
                buf.append ("   ").append (entry.getValue ().getName ()).append (":");
                buf.append (entry.getKey ()).append ("\n");
            }
            buf.append ("  ]\n");
            log (buf.toString ());
            Util.dataLogger ().info (IRainbowHealthProtocol.DATA_ADAPTATION_SCORE + buf.toString ());
        }

        if (_stopWatchForTesting != null) {
            _stopWatchForTesting.stop ();
        }
        if (scoredStrategies.size () > 0) {
            Strategy selectedStrategy = scoredStrategies.get (scoredStrategies.lastKey ());
            return selectedStrategy;
        }
        else {
            Util.dataLogger ().info (IRainbowHealthProtocol.DATA_ADAPTATION_END);
            log ("<< NO applicable strategy, adaptation cycle ended.");
            return null;
//            m_model.clearConstraintViolated ();
        }
    }

    /**
     * Iterate through the supplied set of strategies, compute aggregate attributes, and use the aggregate values plus
     * stakeholder utility preferences to compute an integer score for each Strategy, between 0 and 100.
     * 
     * @param subset
     *            the subset of condition-applicable Strategies to score, in the form of a name-strategy map
     * @return a map of score-strategy pairs, sorted in increasing order by score.
     */
    private SortedMap<Double, Strategy> scoreStrategies (Map<String, Strategy> subset) {
        SortedMap<Double, Strategy> scored = new TreeMap<Double, Strategy> ();
        boolean predictionEnabled = false; //Rainbow.predictionEnabled () && Rainbow.utilityPredictionDuration () > 0;
        double[] conds = null; // store the conditions to output for diagnosis
        double[] condsPred = null; // store predicted conditions
        // find the weights of the applicable scenario
        Map<String, Double> weights = Rainbow.instance ().getRainbowMaster ().preferenceDesc ().weights.get (Rainbow
                .getProperty (RainbowConstants.PROPKEY_SCENARIO));
        for (Strategy strategy : subset.values ()) {
            SortedMap<String, Double> aggAtt = strategy.computeAggregateAttributes ();
            // add the strategy failure history as another attribute
            accountForStrategyHistory (aggAtt, strategy);
            String s = strategy.getName () + aggAtt;
            Util.dataLogger ().info (IRainbowHealthProtocol.DATA_ADAPTATION_STRATEGY_ATTR + s);
            log ("aggAttr: " + s);
            /*
             * compute utility values from attributes that combines values
             * representing current condition, then accumulate the weighted
             * utility sum
             */
            double[] items = new double[aggAtt.size ()];
            double[] itemsPred = new double[aggAtt.size ()];
            if (conds == null) {
                conds = new double[aggAtt.size ()];
            }
            if (condsPred == null) {
                condsPred = new double[aggAtt.size ()];
            }
            int i = 0;
            double score = 0.0;
            double scorePred = 0.0; // score based on predictions
            for (String k : aggAtt.keySet ()) {
                double v = aggAtt.get (k);
                // find the applicable utility function
                UtilityFunction u = m_utils.get (k);
                if (u == null) {
                    log ("Error: attempting to calculate for not existent function: " + k);
                    continue;
                }
                Object condVal = null;
                Object condValPred = null;
                // add attribute value from CURRENT condition to accumulated agg
                // value
                condVal = m_model.getProperty (u.mapping ());
                items[i] = v;
                if (condVal != null && condVal instanceof Double) {
                    m_reportingPort
                    .trace (getComponentType (), "Avg value of prop: " + u.mapping () + " == " + condVal);
                    conds[i] = ((Double )condVal).doubleValue ();
                    items[i] += conds[i];
                }
                // now compute the utility, apply weight, and accumulate to sum
                score += weights.get (k) * u.f (items[i]);

                // if applicable, process the same set of info using predicted
                // values
                if (predictionEnabled) {
                    // add attribute value from FUTURE condition to accumulated
                    // agg value
                    condValPred = m_model.predictProperty (u.mapping (), 0L/*Rainbow.utilityPredictionDuration ()*/);
                    itemsPred[i] = v;
                    if (condValPred != null && condValPred instanceof Double) {
                        // if (m_logger.isTraceEnabled())
                        log ("Avg value of predicted prop: " + u.mapping () + " == " + condValPred);
                        condsPred[i] = ((Double )condValPred).doubleValue ();
                        itemsPred[i] += condsPred[i];
                    }
                    // now compute the utility, apply weight, and accumulate to
                    // sum
                    scorePred += weights.get (k) * u.f (itemsPred[i]);
                }
                ++i;
            }

            if (predictionEnabled) {
                // compare and pick higher score
                if (scorePred > .9 * score) { // score based on prediction
                    // prevails
                    log ("cur-cond score " + score + " was lower, discarding: " + Arrays.toString (items));
                    score = scorePred;
                    items = itemsPred;
                }
            }

            // log this
            s = Arrays.toString (items);
            if (score < m_minUtilityThreshold) {
                // utility score too low, don't consider for adaptation
                log ("score " + score + " below threshold, discarding: " + s);
            }
            else {
                scored.put (score, strategy);
            }
            Util.dataLogger ().info (IRainbowHealthProtocol.DATA_ADAPTATION_STRATEGY_ATTR2 + s);
            log ("aggAtt': " + s);
        }
        log ("cond   : " + Arrays.toString (conds));
        if (predictionEnabled) {
            log ("condP! : " + Arrays.toString (condsPred));
        }
        return scored;
    }

    /**
     * Retrieves the adaptation repertoire; for each tactic, store the respective tactic attribute vectors.
     */
    private void initAdaptationRepertoire () {
        File stitchPath = Util.getRelativeToPath (Rainbow.instance ().getTargetPath (),
                Rainbow.getProperty (RainbowConstants.PROPKEY_SCRIPT_PATH));
        if (stitchPath.exists () && stitchPath.isDirectory ()) {
            FilenameFilter ff = new FilenameFilter () { // find only ".s" files
                @Override
                public boolean accept (File dir, String name) {
                    return name.endsWith (".s");
                }
            };
            for (File f : stitchPath.listFiles (ff)) {
                try {
                    // don't duplicate loading of script files
                    Stitch stitch = Ohana.instance ().findStitch (f.getCanonicalPath ());
                    if (stitch == null) {
                        DummyStitchProblemHandler stitchProblemHandler = new DummyStitchProblemHandler ();
                        stitch = Stitch.newInstance (f.getCanonicalPath (), stitchProblemHandler);
                        Ohana.instance ().parseFile (stitch);
                        reportProblems (f, stitchProblemHandler);

                        // apply attribute vectors to tactics, if available
                        defineAttributes (stitch,
                                Rainbow.instance ().getRainbowMaster ().preferenceDesc ().attributeVectors);
                        m_repertoire.add (stitch);
                        log ("Parsed script " + stitch.path);
                    }
                    else {
                        log ("Previously known script " + stitch.path);
                    }
                }
                catch (IOException e) {
                    m_reportingPort.error (RainbowComponentT.ADAPTATION_MANAGER,
                            "Obtaining file canonical path failed! " + f.getName (), e);
                }
            }
        }
    }

    private void reportProblems (File f, DummyStitchProblemHandler sph) {

        Collection<StitchProblem> problem = sph.getProblems ();
        boolean reported = !problem.isEmpty ();
        if (!problem.isEmpty ()) {
            log ("Errors exist in strategy: " + f.getName () + ", or one of its included files");
        }
        for (StitchProblem p : problem) {
            StringBuffer out = new StringBuffer ();
            switch (p.getSeverity ()) {
            case StitchProblem.ERROR:
                out.append ("ERROR: ");
                break;
            case StitchProblem.WARNING:
                out.append ("WARNING: ");
                break;
            case StitchProblem.FATAL:
                out.append ("FATAL ERROR: ");
                break;
            case StitchProblem.UNKNOWN:
                out.append ("UNKNOWN PROBLEM: ");
                break;
            }
            out.append ("Line: " + p.getLine ());
            out.append (", ");
            out.append (" Column: " + p.getColumn ());
            out.append (": " + p.getMessage ());
            log (out.toString ());
        }
        sph.clearProblems ();
    }

    private void defineAttributes (Stitch stitch, Map<String, Map<String, Object>> attrVectorMap) {
        for (Tactic t : stitch.script.tactics) {
            Map<String, Object> attributes = attrVectorMap.get (t.getName ());
            if (attributes != null) {
                // found attribute def for tactic, save all key-value pairs
                m_reportingPort.trace (getComponentType (), "Found attributes for tactic " + t.getName ()
                        + ", saving pairs...");
                for (Map.Entry<String, Object> e : attributes.entrySet ()) {
                    t.putAttribute (e.getKey (), e.getValue ());
                    m_reportingPort.trace (getComponentType (), " - (" + e.getKey () + ", " + e.getValue () + ")");
                }
            }
        }
    }

    private static final int I_RUN     = 0;
    private static final int I_SUCCESS = 1;
    private static final int I_FAIL    = 2;
    private static final int I_OTHER   = 3;
    private static final int CNT_I     = 4;

    private void tallyStrategyOutcome (Strategy s) {
        if (m_historyTrackUtilName == null) return;

        String name = s.getName ();
        // mark timer of failure, if applicable
        Beacon timer = m_failTimer.get (name);
        if (timer == null) {
            timer = new Beacon ();
            m_failTimer.put (name, timer);
        }
        // get the stats array for this strategy
        int[] stat = m_historyCnt.get (name);
        if (stat == null) {
            stat = new int[CNT_I];
            stat[I_RUN] = 0;
            stat[I_SUCCESS] = 0;
            stat[I_FAIL] = 0;
            stat[I_OTHER] = 0;
            m_historyCnt.put (name, stat);
        }
        // tally outcome counts
        ++stat[I_RUN];
        switch (s.outcome ()) {
        case SUCCESS:
            ++stat[I_SUCCESS];
            break;
        case FAILURE:
            ++stat[I_FAIL];
            timer.mark ();
            break;
        default:
            ++stat[I_OTHER];
            break;
        }
        String str = name + Arrays.toString (stat);
        log ("History: " + str);
        Util.dataLogger ().info (IRainbowHealthProtocol.DATA_ADAPTATION_STAT + str);
    }

    private void accountForStrategyHistory (Map<String, Double> aggAtt, Strategy s) {
        if (m_historyTrackUtilName == null) return;

        if (m_historyCnt.containsKey (s.getName ())) {
            // consider failure only
            aggAtt.put (m_historyTrackUtilName, getFailureRate (s));
        }
        else {
            // consider no failure
            aggAtt.put (m_historyTrackUtilName, 0.0);
        }
    }

    private double getFailureRate (Strategy s) {
        double rv = 0.0;
        if (m_historyTrackUtilName == null) return rv;

        int[] stat = m_historyCnt.get (s.getName ());
        if (stat != null) {
            Beacon timer = m_failTimer.get (s.getName ());
            double factor = 1.0;
            long failTimeDelta = timer.elapsedTime () - FAILURE_EFFECTIVE_WINDOW;
            if (failTimeDelta > 0) {
                factor = FAILURE_WINDOW_CHUNK * 1.0 / failTimeDelta;
            }
            rv = factor * stat[I_FAIL] / stat[I_RUN];
        }
        return rv;
    }

    @Override
    public void onEvent (IModelInstance model, IRainbowMessage message) {
        // Because of the subscription, the model should be the model ref so no need to check
        String typecheckSt = (String )message.getProperty (IModelChangeBusPort.PARAMETER_PROP + "0");
        Boolean typechecks = Boolean.valueOf (typecheckSt);
        // Cause the thread to wake up if it is sleeping
        if (!typechecks) {
            activeThread ().interrupt ();
        }
    }

    @Override
    protected RainbowComponentT getComponentType () {
        return RainbowComponentT.ADAPTATION_MANAGER;
    }


}