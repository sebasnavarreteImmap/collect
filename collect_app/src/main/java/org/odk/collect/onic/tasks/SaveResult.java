/*
 * Copyright (C) 2014 University of Washington
 *
 * Originally developed by Dobility, Inc. (as part of SurveyCTO)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.onic.tasks;

/**
 * Author: Meletis Margaritis
 * Date: 15/3/2013
 * Time: 2:53 μμ
 */
public class SaveResult {

    private int saveResult;
    private boolean complete;
    private String saveErrorMessage;

    public int getSaveResult() {
        return saveResult;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setSaveResult(int saveResult, boolean complete) {
        this.saveResult = saveResult;
        this.complete = complete;
    }

    public void setSaveErrorMessage(String saveErrorMessage) {
        this.saveErrorMessage = saveErrorMessage;
    }

    public String getSaveErrorMessage() {
        return saveErrorMessage;
    }
}
