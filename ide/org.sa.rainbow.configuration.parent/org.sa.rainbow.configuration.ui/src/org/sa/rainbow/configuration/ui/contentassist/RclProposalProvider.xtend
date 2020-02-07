/*
 * generated by Xtext 2.19.0
 */
package org.sa.rainbow.configuration.ui.contentassist
/*
Copyright 2020 Carnegie Mellon University

Permission is hereby granted, free of charge, to any person obtaining a copy of this 
software and associated documentation files (the "Software"), to deal in the Software 
without restriction, including without limitation the rights to use, copy, modify, merge,
 publish, distribute, sublicense, and/or sell copies of the Software, and to permit 
 persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all 
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
DEALINGS IN THE SOFTWARE.
 */

import com.google.common.base.Predicate
import com.google.common.collect.Sets
import com.google.inject.Inject
import com.google.inject.name.Named
import java.util.Collection
import java.util.HashSet
import java.util.List
import java.util.Set
import java.util.stream.Collectors
import javax.swing.text.BadLocationException
import org.eclipse.emf.ecore.EObject
import org.eclipse.jface.viewers.StyledString
import org.eclipse.xtext.CrossReference
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.Grammar
import org.eclipse.xtext.GrammarUtil
import org.eclipse.xtext.Group
import org.eclipse.xtext.Keyword
import org.eclipse.xtext.RuleCall
import org.eclipse.xtext.common.types.access.IJvmTypeProvider
import org.eclipse.xtext.common.types.xtext.ui.ITypesProposalProvider
import org.eclipse.xtext.common.types.xtext.ui.TypeMatchFilters
import org.eclipse.xtext.resource.IEObjectDescription
import org.eclipse.xtext.ui.editor.contentassist.ConfigurableCompletionProposal
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor
import org.eclipse.xtext.xbase.lib.Functions.Function1
import org.sa.rainbow.configuration.ConfigAttributeConstants
import org.sa.rainbow.configuration.XtendUtils
import org.sa.rainbow.configuration.rcl.Array
import org.sa.rainbow.configuration.rcl.Assignment
import org.sa.rainbow.configuration.rcl.BooleanLiteral
import org.sa.rainbow.configuration.rcl.Component
import org.sa.rainbow.configuration.rcl.ComponentType
import org.sa.rainbow.configuration.rcl.DeclaredProperty
import org.sa.rainbow.configuration.rcl.DoubleLiteral
import org.sa.rainbow.configuration.rcl.Effector
import org.sa.rainbow.configuration.rcl.Factory
import org.sa.rainbow.configuration.rcl.Gauge
import org.sa.rainbow.configuration.rcl.GaugeType
import org.sa.rainbow.configuration.rcl.IPLiteral
import org.sa.rainbow.configuration.rcl.IntegerLiteral
import org.sa.rainbow.configuration.rcl.Probe
import org.sa.rainbow.configuration.rcl.PropertyReference
import org.sa.rainbow.configuration.rcl.RclPackage
import org.sa.rainbow.configuration.rcl.StringLiteral
import org.sa.rainbow.configuration.services.RclGrammarAccess
import org.sa.rainbow.core.gauges.AbstractGauge
import org.sa.rainbow.core.models.IModelInstance
import org.sa.rainbow.core.models.commands.AbstractLoadModelCmd
import org.sa.rainbow.core.models.commands.AbstractSaveModelCmd
import org.sa.rainbow.core.models.commands.ModelCommandFactory
import org.sa.rainbow.model.acme.AcmeModelOperation
import org.sa.rainbow.translator.probes.AbstractProbe

/**
 * See https://www.eclipse.org/Xtext/documentation/304_ide_concepts.html#content-assist
 * on how to customize the content assistant.
 */
class RclProposalProvider extends AbstractRclProposalProvider {
	
	@Inject
	RclGrammarAccess cfGrammar
	
	static var Set<String> KEYWORDS = null
	def isKeyword(String s) {
		if (KEYWORDS === null) {
			KEYWORDS = newHashSet
			val Set<Grammar> grammars = newHashSet
			grammars.add(cfGrammar.grammar)
			grammars.addAll(GrammarUtil.allUsedGrammars(cfGrammar.grammar));
			for (g : grammars) {
				KEYWORDS.addAll(GrammarUtil.getAllKeywords(g))
			}
		}
		return KEYWORDS.contains(s)
	}

	override complete_RICH_TEXT_DQ(EObject model, RuleCall ruleCall, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		completeInRichString(model, ruleCall, context, acceptor, "\"")
	}

	override complete_RICH_TEXT_START_DQ(EObject model, RuleCall ruleCall, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		completeInRichString(model, ruleCall, context, acceptor, "\"")
	}

	override complete_RICH_TEXT_END_DQ(EObject model, RuleCall ruleCall, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		completeInRichString(model, ruleCall, context, acceptor, "\"")
	}

	override complete_RICH_TEXT_INBETWEEN_DQ(EObject model, RuleCall ruleCall, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		completeInRichString(model, ruleCall, context, acceptor, "\"")
	}

//	override complete_RICH_TEXT_SQ(EObject model, RuleCall ruleCall, ContentAssistContext context,
//		ICompletionProposalAcceptor acceptor) {
//		completeInRichString(model, ruleCall, context, acceptor, "'")
//	}
//
//	override complete_RICH_TEXT_START_SQ(EObject model, RuleCall ruleCall, ContentAssistContext context,
//		ICompletionProposalAcceptor acceptor) {
//		completeInRichString(model, ruleCall, context, acceptor, "'")
//	}
//
//	override complete_RICH_TEXT_END_SQ(EObject model, RuleCall ruleCall, ContentAssistContext context,
//		ICompletionProposalAcceptor acceptor) {
//		completeInRichString(model, ruleCall, context, acceptor,"'")
//	}
//
//	override complete_RICH_TEXT_INBETWEEN_SQ(EObject model, RuleCall ruleCall, ContentAssistContext context,
//		ICompletionProposalAcceptor acceptor) {
//		completeInRichString(model, ruleCall, context, acceptor, "'")
//	}
	def completeInRichString(EObject object, RuleCall call, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor, String delimiter) {
		val node = context.getCurrentNode();
		val textRegion = node.textRegion;
		val offset = textRegion.offset
		val length = textRegion.length
		val currentNodeText = node.text
		if (currentNodeText.startsWith("\u00BB") && offset + 1 <= context.offset ||
			currentNodeText.startsWith(delimiter) && offset + delimiter.length <= context.offset) {
			if (context.offset > offset && context.offset < offset + length) {
				addGuillemotsProposal(context, acceptor)
			}
		} else if (currentNodeText.startsWith("\u00AB\u00AB")) {
			try {
				val document = context.viewer.document
				val nodeLine = document.getLineOffset(offset)
				val completionLine = document.getLineOffset(context.offset)
				if (completionLine > nodeLine) {
					addGuillemotsProposal(context, acceptor)
				}
			} catch (BadLocationException e) {
			}
		}
	}

	def addGuillemotsProposal(ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		acceptor.accept(
			new ConfigurableCompletionProposal("\u00AB\u00BB", context.getOffset(), context.getSelectedText().length(),
				1));
	}

	override complete_PropertyReference(EObject model, RuleCall ruleCall, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		super.complete_PropertyReference(model, ruleCall, context, acceptor)
		val proposal = new ConfigurableCompletionProposal("\u00AB\u00AB\u00BB\u00BB", context.getOffset(),
			context.getSelectedText().length(), 2)
		proposal.priority = 1
		acceptor.accept(proposal)
	}

	override complete_Assignment(EObject model, RuleCall ruleCall, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {

		// Check Gauge attributes
		var parent = model
		var assignmentContext = null as Assignment
		while (parent !== null &&
			(!(parent instanceof Gauge) && !(parent instanceof Probe) && !(parent instanceof GaugeType) &&
				!(parent instanceof Effector))) {
			parent = parent.eContainer
			if (parent instanceof Assignment) {
				assignmentContext = parent as Assignment
			}
		}

		val allPossibleFields = new HashSet<String>()
		if (parent instanceof Gauge || parent instanceof GaugeType) {
			if (assignmentContext === null) {
				allPossibleFields.addAll(ConfigAttributeConstants.ALL_OFREQUIRED_GAUGE_SUBFILEDS.keySet)
				allPossibleFields.addAll(ConfigAttributeConstants.OPTIONAL_GUAGE_FIELDS)
				if (parent instanceof Gauge) {
					allPossibleFields.removeAll((parent as Gauge).body.assignment.map[it.name])
				} else {
					allPossibleFields.removeAll((parent as GaugeType).body.assignment.map[it.name])
				}
			} else {
				var subfields = ConfigAttributeConstants.ALL_OFREQUIRED_GAUGE_SUBFILEDS.get(assignmentContext.name)
				if (subfields !== null) {
					allPossibleFields.addAll(subfields)
				}
				subfields = ConfigAttributeConstants.OPTIONAL_GAUGE_SUBFIELDS.get(assignmentContext.name)
				if (subfields !== null) {
					allPossibleFields.addAll(subfields)
				}
				allPossibleFields.removeAll((assignmentContext.value.value as Component).assignment.map[it.name])
			}
		} else if (parent instanceof Probe) {
			if (assignmentContext === null) {
				allPossibleFields.addAll(ConfigAttributeConstants.ALL_OFREQUIRED_PROBE_FIELDS)
				allPossibleFields.addAll(ConfigAttributeConstants.ONE_OFREQUIRED_PROBE_FIELDS);
				allPossibleFields.removeAll((parent as Probe).properties.assignment.map[it.name])
			} else {
				var subfields = ConfigAttributeConstants.ALL_OFREQUIRED_PROBE_SUBFIELDS.get(assignmentContext.name)
				if (subfields !== null) {
					allPossibleFields.addAll(subfields)
				}
				subfields = ConfigAttributeConstants.OPTIONAL_PROBE_SUBFIELDS.get(assignmentContext.name)
				if (subfields !== null) {
					allPossibleFields.addAll(subfields)
				}
				allPossibleFields.removeAll((assignmentContext.value.value as Component).assignment.map[it.name])
			}
		} else if (parent instanceof Effector) {
			if (assignmentContext === null) {
				allPossibleFields.addAll(ConfigAttributeConstants.ALL_OFREQUIRED_EFFECTOR_FIELDS)
				allPossibleFields.addAll(ConfigAttributeConstants.ONE_OFREQUIRED_EFFECTOR_FIELDS)
				allPossibleFields.removeAll((parent as Effector).body.assignment.map[it.name])
			} else {
				var subfields = ConfigAttributeConstants.ALL_OFREQUIRED_EFFECTOR_SUBFIELDS.get(assignmentContext.name)
				if (subfields !== null) {
					allPossibleFields.addAll(subfields)
				}
				subfields = ConfigAttributeConstants.OPTIONAL_EFFECTOR_SUBFIELDS.get(assignmentContext.name)
				if (subfields !== null) {
					allPossibleFields.addAll(subfields)
				}
				allPossibleFields.removeAll((assignmentContext.value.value as Component).assignment.map[it.name])
			}
		} else {
			parent = EcoreUtil2.getContainerOfType(model, DeclaredProperty)
			if ((parent as DeclaredProperty)?.component == ComponentType.UTILITY) {
				val sb = new StringBuffer()
				var eContainer = model
				while (eContainer != parent) {
					eContainer = eContainer.eContainer
					if (eContainer instanceof Assignment) {
						sb.insert(0, ':')
						val par = (eContainer as Assignment)
						if (ConfigAttributeConstants.UTILITY_PROPERTY_TYPES.containsKey(par.name)) {
							sb.insert(0, par.name)
						}
					}
				}
				val container = sb.toString
				for (key : ConfigAttributeConstants.UTILITY_PROPERTY_TYPES.keySet) {
					if (key.startsWith(container)) {
						if (container.contains(':')) {
							val split = key.split(':')
							allPossibleFields.add(split.get(split.length - 1))
						} else {
							allPossibleFields.add(key)
						}
					}
				}
			}
			else if ((parent as DeclaredProperty)?.component == ComponentType.GUI) {
				var m = model;
				if (m instanceof Assignment && m.eContainer instanceof Component) m = m.eContainer
				allPossibleFields.addAll(XtendUtils.getPropertySuggestions(ConfigAttributeConstants.GUI_PROPERTY_TUPES, m))
			}
		}
		var suggestions = allPossibleFields.stream.filter[it.startsWith(context.prefix)].collect(Collectors.toSet)
		suggestions.forEach [
			{
				val proposal = new ConfigurableCompletionProposal((isKeyword(it)?'''^«it»''':it) + " = ", context.replaceRegion.offset,
					context.replaceRegion.length, it.length + 3)
				proposal.displayString = it
				proposal.priority = 1
				acceptor.accept(proposal)
			}
		]
		if (suggestions.empty) {
			super.complete_Assignment(model, ruleCall, context, acceptor)

		}
	}

	@Inject
	@Named(value="jvmtypes")
	private IJvmTypeProvider.Factory jvmTypeProviderFactory;
	@Inject
	@Named(value="jvmtypes")
	private ITypesProposalProvider typeProposalProvider;

	override completeGaugeTypeBody_Mcf(EObject model, org.eclipse.xtext.Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {

		val jvmTypeProvider = jvmTypeProviderFactory.createTypeProvider(model.eResource.resourceSet)
		val subclass = jvmTypeProvider.findTypeByName(ModelCommandFactory.name)
		typeProposalProvider.createSubTypeProposals(subclass, this, context,
			RclPackage.Literals.GAUGE_TYPE_BODY__MCF, TypeMatchFilters.canInstantiate, acceptor);
		val v = model.eResource.resourceSet.resources
		val models = new HashSet<String>();
		for (r : v) {
			val res = r.allContents
			res.forEach [
				if (it instanceof Factory) {
					models.add((it as Factory).name)
				}
			]
		}
		fillInProposals(models, acceptor, context)

	}

	override completeFactoryDefinition_LoadCmd(EObject model, org.eclipse.xtext.Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		val jvmTypeProvider = jvmTypeProviderFactory.createTypeProvider(model.eResource.resourceSet)
		val subclass = jvmTypeProvider.findTypeByName(AbstractLoadModelCmd.name)
		typeProposalProvider.createSubTypeProposals(subclass, this, context,
			RclPackage.Literals.FACTORY_DEFINITION__LOAD_CMD, TypeMatchFilters.canInstantiate, acceptor);

	}

	override completeFactoryDefinition_SaveCmd(EObject model, org.eclipse.xtext.Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		val jvmTypeProvider = jvmTypeProviderFactory.createTypeProvider(model.eResource.resourceSet)
		val subclass = jvmTypeProvider.findTypeByName(AbstractSaveModelCmd.name)
		typeProposalProvider.createSubTypeProposals(subclass, this, context,
			RclPackage.Literals.FACTORY_DEFINITION__SAVE_CMD, TypeMatchFilters.canInstantiate, acceptor);

	}

	override completeFactoryDefinition_Extends(EObject model, org.eclipse.xtext.Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		val jvmTypeProvider = jvmTypeProviderFactory.createTypeProvider(model.eResource.resourceSet)
		val subclass = jvmTypeProvider.findTypeByName(ModelCommandFactory.name)
		typeProposalProvider.createSubTypeProposals(subclass, this, context,
			RclPackage.Literals.FACTORY_DEFINITION__EXTENDS, TypeMatchFilters.canInstantiate, acceptor);
	}
	
	override completeFactoryDefinition_ModelClass(EObject model, org.eclipse.xtext.Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		val jvmTypeProvider = jvmTypeProviderFactory.createTypeProvider(model.eResource.resourceSet)
		val subclass = jvmTypeProvider.findTypeByName(IModelInstance.name)
		typeProposalProvider.createSubTypeProposals(subclass, this, context,
			RclPackage.Literals.FACTORY_DEFINITION__MODEL_CLASS, TypeMatchFilters.canInstantiate, acceptor);
		
    }	

	override completeEffectorBody_Ref(EObject model, org.eclipse.xtext.Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		val v = model.eResource.resourceSet.resources
		val models = new HashSet<String>();
		for (r : v) {
			val res = r.allContents
			res.forEach [
				if (it instanceof DeclaredProperty && (it as DeclaredProperty).component == ComponentType.MODEL) {
					models.add((it as DeclaredProperty).name)
				}
			]
		}
		if (!models.empty) {
			models.forEach [
				acceptor.accept(
					new ConfigurableCompletionProposal("\u00AB\u00AB" + it + "\u00BB\u00BB",
						context.replaceRegion.offset, context.replaceRegion.length, it.length + 4, null,
						new StyledString(it), null, null))
			]
			acceptor.accept(
				new ConfigurableCompletionProposal("\u00AB\u00AB\u00BB\u00BB", context.getOffset(),
					context.getSelectedText().length(), 2));
		}
	}

	override completeGaugeBody_Ref(EObject model, org.eclipse.xtext.Assignment assignment, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		val v = model.eResource.resourceSet.resources
		val models = new HashSet<String>();
		for (r : v) {
			val res = r.allContents
			res.forEach [
				if (it instanceof DeclaredProperty && (it as DeclaredProperty).component == ComponentType.MODEL) {
					models.add((it as DeclaredProperty).name)
				}
			]
		}
		fillInProposals(models, acceptor, context)
	}
	
	protected def void fillInProposals(HashSet<String> names, ICompletionProposalAcceptor acceptor, ContentAssistContext context) {
		if (!names.empty) {
			names.forEach [
				acceptor.accept(
					new ConfigurableCompletionProposal("\u00AB\u00AB" + it + "\u00BB\u00BB",
						context.replaceRegion.offset, context.replaceRegion.length, it.length + 4, null,
						new StyledString(it), null, null))
			]
			acceptor.accept(
				new ConfigurableCompletionProposal("\u00AB\u00AB\u00BB\u00BB", context.getOffset(),
					context.getSelectedText().length(), 2));
		}
	}

	override completeDeclaredProperty_Default(EObject model, org.eclipse.xtext.Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		val ass = EcoreUtil2.getContainerOfType(model, DeclaredProperty)
		if (ass.name.matches(".*class_[0-9]+$")) {
			val checkName = ass.name.substring(0, ass.name.lastIndexOf('_')) + "*"
			val subclass = ConfigAttributeConstants.PROPERTY_VALUE_CLASSES.get(checkName)
			if (subclass !== null) {
				val jvmTypeProvider = jvmTypeProviderFactory.createTypeProvider(model.eResource.resourceSet)
				val sc = jvmTypeProvider.findTypeByName(subclass)
				if (sc !== null) {
					typeProposalProvider.createSubTypeProposals(sc, this, context,
						RclPackage.Literals.DECLARED_PROPERTY__DEFAULT, TypeMatchFilters.canInstantiate,
						acceptor);
					pauseAssisting(acceptor);
					return
				}
			}
		}

		super.completeDeclaredProperty_Default(model, assignment, context, acceptor)
	}

	override completeAssignment_Value(EObject model, org.eclipse.xtext.Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		val ass = EcoreUtil2.getContainerOfType(model, Assignment)
		if (ass.name == "javaClass") {
			val gtc = EcoreUtil2.getContainerOfType(ass, GaugeType)
			val gc = EcoreUtil2.getContainerOfType(ass, Gauge)
			if (gtc !== null || gc !== null) {
				val jvmTypeProvider = jvmTypeProviderFactory.createTypeProvider(model.eResource.resourceSet)
				val subclass = jvmTypeProvider.findTypeByName(AbstractGauge.name)
				if (subclass != null) {
					typeProposalProvider.createSubTypeProposals(subclass, this, context,
						RclPackage.Literals.ASSIGNMENT__VALUE, TypeMatchFilters.canInstantiate, acceptor);
					pauseAssisting(acceptor);
					return

				}
			}
			val pc = EcoreUtil2.getContainerOfType(ass, Probe);
			if (pc !== null) {
				val jvmTypeProvider = jvmTypeProviderFactory.createTypeProvider(model.eResource.resourceSet)
				val subclass = jvmTypeProvider.findTypeByName(AbstractProbe.name)
				if (subclass !== null) {
					typeProposalProvider.createSubTypeProposals(subclass, this, context,
						RclPackage.Literals.ASSIGNMENT__VALUE, TypeMatchFilters.canInstantiate, acceptor);
					pauseAssisting(acceptor);
					return
				}

			}
		}
		val parentProp = EcoreUtil2.getContainerOfType(ass, DeclaredProperty)
		if (parentProp.component == ComponentType.GUI) {
			var parent = EcoreUtil2.getContainerOfType(model.eContainer, Assignment);
			var name = ass.name
			while (parent !== null) {
				name = parent.name + ":" + name
				parent = EcoreUtil2.getContainerOfType(parent.eContainer, Assignment);
			}
			val valVunc = ConfigAttributeConstants.GUI_PROPERTY_TUPES.get(name)?.get('values') as Function1<Assignment,Collection<String>>
			if (valVunc !== null) {
				val values = (valVunc).apply(ass)
				if (values !== null) {
					values.forEach[
						acceptor.accept(
							new ConfigurableCompletionProposal(it, context.replaceRegion.offset,
								context.replaceRegion.length, it.length))
					]
				}
			
			} 
			
			val extends = ConfigAttributeConstants.GUI_PROPERTY_TUPES.get(name)?.get('extends') as List<Class>
			processPropertySuggestionsBasedOnClass(extends, model, context, acceptor)
		}
		else if (parentProp?.component !== null) {
			var extends = ConfigAttributeConstants.COMPONENT_PROPERTY_TYPES.get(parentProp.component)?.get(ass.name)?.
				get('extends') as List<Class>
			if (extends != null) {
				processPropertySuggestionsBasedOnClass(extends, model, context, acceptor)
			} else if (parentProp.component === ComponentType.UTILITY) {
				val sb = new StringBuffer(ass.name)
				var eContainer = ass as EObject
				while (eContainer !== null) {
					eContainer = eContainer.eContainer
					if (eContainer instanceof Assignment) {
						sb.insert(0, ':')
						val par = (eContainer as Assignment)
						if (ConfigAttributeConstants.UTILITY_PROPERTY_TYPES.containsKey(par.name)) {
							sb.insert(0, par.name)
						}
					}
				}
				extends = ConfigAttributeConstants.UTILITY_PROPERTY_TYPES.containsKey(
					sb.toString) ? ConfigAttributeConstants.UTILITY_PROPERTY_TYPES.get(sb.toString).get(
					"extends") as List<Class> : null
				processPropertySuggestionsBasedOnClass(extends, model, context, acceptor)
			}
			return
		}
		val probe = EcoreUtil2.getContainerOfType(model, Probe)
		if (probe !== null) {
			val parent = EcoreUtil2.getContainerOfType(model.eContainer, Assignment)
			var name = ass.name
			if (parent !== null) {
				name = parent.name + ":" + ass.name
			}
			val extends = ConfigAttributeConstants.PROBE_PROPERTY_TYPES.get(name)?.get('extends') as List<Class>
			processPropertySuggestionsBasedOnClass(extends, model, context, acceptor)
			return
		}
		val gauge = EcoreUtil2.getContainerOfType(model, Gauge)
		val gaugeType = EcoreUtil2.getContainerOfType(model, GaugeType)
		if (gauge !== null || gaugeType !== null) {
			val parent = EcoreUtil2.getContainerOfType(model.eContainer, Assignment)
			var name = ass.name
			if (parent !== null) {
				name = parent.name + ":" + ass.name
			}
			val extends = ConfigAttributeConstants.GAUGE_PROPERTY_TYPES.get(name)?.get('extends') as List<Class>
			processPropertySuggestionsBasedOnClass(extends, model, context, acceptor)
			return
		}
		val effector = EcoreUtil2.getContainerOfType(model, Effector)
		if (effector !== null) {
			val parent = EcoreUtil2.getContainerOfType(model.eContainer, Assignment);
			var name = ass.name
			if (parent !== null) {
				name = parent.name + ":" + ass.name
			}
			val extends = ConfigAttributeConstants.EFFECTOR_PROPERTY_TYPES.get(name)?.get('extends') as List<Class>
			processPropertySuggestionsBasedOnClass(extends, model, context, acceptor)
			return
		}
		

		super.completeAssignment_Value(model, assignment, context, acceptor)
	}

	def processPropertySuggestionsBasedOnClass(List<Class> ext, EObject model, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		if (ext !== null) {
			val jvmTypeProvider = jvmTypeProviderFactory.createTypeProvider(model.eResource.resourceSet)
			for (class : ext) {
				if (class == StringLiteral) {
					acceptor.accept(
						new ConfigurableCompletionProposal('""', context.replaceRegion.offset,
							context.replaceRegion.length, 1))
				} else if (class == BooleanLiteral) {
					acceptor.accept(
						new ConfigurableCompletionProposal('true', context.replaceRegion.offset,
							context.replaceRegion.length, 4))
					acceptor.accept(
						new ConfigurableCompletionProposal('false', context.replaceRegion.offset,
							context.replaceRegion.length, 4))
				} else if (class == PropertyReference) {
					acceptor.accept(
						new ConfigurableCompletionProposal("\u00AB\u00AB\u00BB\u00BB", context.getOffset(),
							context.getSelectedText().length(), 2));
				} else if (class == Probe) {
						val v = model.eResource.resourceSet.resources
						val probes = newHashSet
						for (r : v) {
							val res = r.allContents
							res.forEach [
								if (it instanceof Probe) {
									probes.add((it as Probe).name)
								}
							]
							
						}
						fillInProposals(probes, acceptor, context)
				}
				else if (class == Factory) {
					val names = newHashSet
					val v = model.eResource.resourceSet.resources
					for (r : v) {
						val res = r.allContents
						res.forEach[
							if (it instanceof Factory) {
								names.add((it as Factory).name)
							}
						]
					}
					fillInProposals(names, acceptor, context)
				} else if (class==GaugeType) {
					val names = newHashSet
					val v = model.eResource.resourceSet.resources
					for (r : v) {
						val res = r.allContents
						res.forEach[
							if (it instanceof GaugeType) {
								names.add((it as GaugeType).name)
							}
						]
					}
					fillInProposals(names, acceptor, context)
/*				} else if (class == ProbeReference) {
					acceptor.accept(
						new ConfigurableCompletionProposal("probe ", context.getOffset(),
							context.getSelectedText().length(), 6));
				*/} else if (class == Array) {
					acceptor.accept(
						new ConfigurableCompletionProposal("[]", context.getOffset(),
							context.getSelectedText().length(), 1));
				} else if (class == Component) {
					acceptor.accept(
						new ConfigurableCompletionProposal("{}", context.getOffset(),
							context.getSelectedText().length(), 1));
				} else if (class == DoubleLiteral || class == IntegerLiteral || class == IPLiteral) {
				} else {
					val sc = jvmTypeProvider.findTypeByName(class.name)
					if (sc !== null) {
						typeProposalProvider.createSubTypeProposals(sc, this, context,
							RclPackage.Literals.DECLARED_PROPERTY__DEFAULT, TypeMatchFilters.canInstantiate,
							acceptor);
					}
				}

			}
			pauseAssisting(acceptor);
			return
		}
	}
	
	override completeCommandDefinition_Cmd(EObject model, org.eclipse.xtext.Assignment assignment, 
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		val jvmTypeProvider = jvmTypeProviderFactory.createTypeProvider(model.eResource.resourceSet)
		val sc = jvmTypeProvider.findTypeByName(typeof(AcmeModelOperation).name)
		if (sc !== null) {
			typeProposalProvider.createSubTypeProposals(sc, this, context,
							RclPackage.Literals.COMMAND_DEFINITION__CMD, TypeMatchFilters.canInstantiate,
							acceptor);
		}
		pauseAssisting(acceptor)
		
	}
	
	override completeCommandReference_Command(EObject model, org.eclipse.xtext.Assignment assignment, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeCommandReference_Command(model, assignment, context, acceptor)
	}
	

//	override completeGaugeBody_Ref(EObject model, org.eclipse.xtext.Assignment assignment, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
//		super.completeGaugeBody_Ref(model, assignment, context, acceptor)
//	}
	override completePropertyReference_Referable(EObject model, org.eclipse.xtext.Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		val parentProp = EcoreUtil2.getContainerOfType(model, DeclaredProperty)
		var myProp = EcoreUtil2.getContainerOfType(model, Assignment)
		if ('model' == myProp.name &&
			(parentProp.component === ComponentType.EXECUTOR || parentProp.component === ComponentType.MANAGER)) {
			val v = model.eResource.resourceSet.resources
			val models = new HashSet<String>();
			for (r : v) {
				val res = r.allContents
				res.forEach [
					if (it instanceof DeclaredProperty && (it as DeclaredProperty).component == ComponentType.MODEL) {
						models.add((it as DeclaredProperty).name)
					}
				]
			}
			if (!models.empty) {
				models.forEach [
					acceptor.accept(
						new ConfigurableCompletionProposal(it, context.replaceRegion.offset,
							context.replaceRegion.length, it.length))
				]
			}
			return;
		}
		val ass = EcoreUtil2.getContainerOfType(model, Assignment)
		val gauge = EcoreUtil2.getContainerOfType(model, Gauge)
		val gaugeType = EcoreUtil2.getContainerOfType(model, GaugeType)
		if (gauge !== null || gaugeType !== null) {
			val parent = EcoreUtil2.getContainerOfType(model.eContainer, Assignment)
			var name = ass.name
			if (parent !== null) {
				name = parent.name + ":" + ass.name
			}
			val extends = ConfigAttributeConstants.GAUGE_PROPERTY_TYPES.get(name)?.get('extends') as List<Class>
			for (class : extends) {
				lookupCrossReference(assignment.getTerminal() as CrossReference, context, acceptor, 
					[IEObjectDescription e | 
						true
					]
				)
			}
			return
		}
		super.completePropertyReference_Referable(model, assignment, context, acceptor)
	}

	def pauseAssisting(ICompletionProposalAcceptor acceptor) {
		if (acceptor instanceof NonContinuingAcceptor) {
			(acceptor as NonContinuingAcceptor).acceptMoreProposals = false
		}
	}

//	override completeReference_Referable(EObject model, org.eclipse.xtext.Assignment assignment, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
//		val ass = EcoreUtil2.getContainerOfType(model, Assignment)
//		if (ass.name != "javaClass")
//			super.completeReference_Referable(model, assignment, context,acceptor);
//	}
	def Predicate<IEObjectDescription> subclassFilter(Class superclass) {
		return new Predicate<IEObjectDescription>() {

			override apply(IEObjectDescription subclass) {
				return true;
			}

		}
	}

	private Set<List<?>> handledArguments;

	override createProposals(ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		try {
			handledArguments = Sets.newHashSet();
			val myAcceptor = new NonContinuingAcceptor(acceptor);
			val selector = createSelector(context, myAcceptor)
			context.firstSetGrammarElements.takeWhile[myAcceptor.canAcceptMoreProposals].forEach[selector.accept(it)]

		} finally {
			handledArguments = null
		}

	}

	override protected announceProcessing(List<?> key) {
		return handledArguments.add(key);
	}
	
	// Keyword group stuff
	@Inject extension RclGrammarAccess
	

	override complete_ModelFactory(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		modelFactoryAccess.group.createKeywordProposal(context, acceptor)
	}
	
	override complete_CommandLoad(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		commandLoadAccess.group.createKeywordProposal(context, acceptor)
	}
	
	override complete_CommandSave(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		commandSaveAccess.group.createKeywordProposal(context, acceptor)
	}
	
	override complete_ModelTypeKW(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		modelTypeKWAccess.group.createKeywordProposal(context, acceptor)
	}
	
	def createKeywordProposal(Group group, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		if (group === null) {
			return null
		}
		val proposalString = group.elements.filter(Keyword).map[value].join(" ") + " "
		acceptor.accept(createCompletionProposal(proposalString, proposalString, null, context))
	}

}
