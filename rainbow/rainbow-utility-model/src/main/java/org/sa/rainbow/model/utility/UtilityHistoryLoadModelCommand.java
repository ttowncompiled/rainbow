/*
 * The MIT License
 *
 * Copyright 2014 CMU ABLE Group.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.sa.rainbow.model.utility;

import java.io.InputStream;

import org.sa.rainbow.core.error.RainbowException;
import org.sa.rainbow.core.models.IModelInstance;
import org.sa.rainbow.core.models.ModelReference;
import org.sa.rainbow.core.models.ModelsManager;
import org.sa.rainbow.core.models.commands.AbstractLoadModelCmd;

public class UtilityHistoryLoadModelCommand extends AbstractLoadModelCmd<UtilityHistory> {

    private String                      m_name;
    private InputStream                 m_stream;
    private UtilityHistoryModelInstance m_result;

    public UtilityHistoryLoadModelCommand (String modelName, ModelsManager mm, InputStream stream, String source) {
        super ("loadUtilityHistory", mm, modelName, stream, source);
        m_name = modelName;
        m_stream = stream;
    }

    @Override
    public IModelInstance<UtilityHistory> getResult () throws IllegalStateException {
        return m_result;
    }

    @Override
    public ModelReference getModelReference () {
        return new ModelReference (m_name, UtilityHistoryModelInstance.UTILITY_HISTORY_TYPE);
    }

    @Override
    protected void subExecute () throws RainbowException {
        UtilityHistory h = new UtilityHistory (new ModelReference (getModelReference ().getModelName (), "Acme"));
        m_result = new UtilityHistoryModelInstance (h, getOriginalSource ());
        doPostExecute ();
    }

    @Override
    protected void subRedo () throws RainbowException {
        doPostExecute ();
    }

    @Override
    protected void subUndo () throws RainbowException {
        doPostUndo ();
    }

    @Override
    protected boolean checkModelValidForCommand (Object model) {
        return true;
    }

}
