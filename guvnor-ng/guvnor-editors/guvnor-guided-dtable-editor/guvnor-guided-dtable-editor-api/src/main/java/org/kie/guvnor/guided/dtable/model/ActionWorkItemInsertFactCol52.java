/*
 * Copyright 2011 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.guvnor.guided.dtable.model;

import org.jboss.errai.common.client.api.annotations.Portable;

/**
 * An Action to insert and set a field value on a new Fact with the value of a
 * Work Item Definition's result parameter
 */
@Portable
public class ActionWorkItemInsertFactCol52 extends ActionInsertFactCol52 {

    private static final long serialVersionUID = 540L;

    private String workItemName;

    private String workItemResultParameterName;

    private String parameterClassName;

    public String getWorkItemName() {
        return workItemName;
    }

    public void setWorkItemName( String workItemName ) {
        this.workItemName = workItemName;
    }

    public String getWorkItemResultParameterName() {
        return workItemResultParameterName;
    }

    public void setWorkItemResultParameterName( String workItemResultParameterName ) {
        this.workItemResultParameterName = workItemResultParameterName;
    }

    public String getParameterClassName() {
        return parameterClassName;
    }

    public void setParameterClassName( String parameterClassName ) {
        this.parameterClassName = parameterClassName;
    }

}
