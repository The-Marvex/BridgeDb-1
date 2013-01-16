// BridgeDb,
// An abstraction layer for identifier mapping services, both local and online.
//
// Copyright 2006-2009  BridgeDb developers
// Copyright 2012-2013  Christian Y. A. Brenninkmeijer
// Copyright 2012-2013  OpenPhacts
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.bridgedb.tools.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bridgedb.utils.BridgeDBException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * 
 * @author Christian
 */
public abstract class HasChildrenMetaData  extends MetaDataBase implements MetaData{
    
    final List<MetaDataBase> childMetaData;

    //HasChildrenMetaData(Element element) throws BridgeDBException{
    //    super(element);
    //    childMetaData = MetaDataRegistry.getChildMetaData(element);
    //}

    HasChildrenMetaData(String name, String type, RequirementLevel requirementLevel, List<MetaDataBase> childMetaData){
        super(name, type, requirementLevel);
        this.childMetaData = childMetaData;
    }
    
    HasChildrenMetaData(HasChildrenMetaData other){
        super(other);
        childMetaData = new ArrayList<MetaDataBase>();
        for (MetaDataBase child:other.childMetaData){
            childMetaData.add(child.getSchemaClone());
        }

    }
    
    // ** MetaData methods 
    
    @Override
    public Set<Value> getValuesByPredicate(URI predicate) {
        for (MetaDataBase child:childMetaData){
            Set<Value> possible = child.getValuesByPredicate(predicate);
            if (possible != null){
                return possible;
            }
        }
        return null;
    }
    
    @Override
    public Set<ResourceMetaData> getResoucresByPredicate(URI predicate){
        for (MetaDataBase child:childMetaData){
            Set<ResourceMetaData> possible = child.getResoucresByPredicate(predicate);
            if (possible != null && !possible.isEmpty()){
                return possible;
            }
        }
        return null;
    }
        

    @Override
    public boolean hasCorrectTypes() throws BridgeDBException {
        for (MetaDataBase child:childMetaData){
            if (!child.hasCorrectTypes()){
                return false;
            }
        }
        return true;
    }
    
    @Override
    public boolean hasRequiredValues() {
        for (MetaDataBase child:childMetaData){
            if (!child.hasRequiredValues()){
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean allStatementsUsed() {
        //A group has values only if all children do.
        for (MetaDataBase child:childMetaData){
            if (!child.allStatementsUsed()) { 
                return false; 
            }
        }
        return true;
    }

    public Set<Statement> getRDF(){
        HashSet results = new HashSet<Statement>();
        for (MetaDataBase child:childMetaData){
            results.addAll(child.getRDF());
        }
        return results;
     }
    
    // ** MetaDataBase methods 
    @Override
    void loadValues(Resource id, Set<Statement> data, MetaDataCollection collection) {
        setupValues(id);
        for (MetaDataBase child:childMetaData){
            child.loadValues(id, data, collection);
        }
    }

    @Override
    boolean hasValues() {
        for (MetaDataBase child:childMetaData){
            if (!child.hasValues()) { 
                return false; 
            }
        }
        return true;
    }
    
    Set<LeafMetaData> getLeaves() {
        Set<LeafMetaData> leaves = new HashSet<LeafMetaData>();
        for (MetaDataBase child:childMetaData){
            leaves.addAll(child.getLeaves());
        }
        return leaves;
    }

    LeafMetaData getLeafByPredicate(URI predicate) {
        for (MetaDataBase child:childMetaData){
            LeafMetaData leaf = child.getLeafByPredicate(predicate);
            if (leaf != null){
                return leaf;
            }
        }
        return null;
    }
    
    // ** AppendBase methods 
    
    @Override
    void appendSchema(StringBuilder builder, int tabLevel) {
        appendSpecific(builder, tabLevel);
        for (MetaDataBase child:childMetaData){
            child.appendSchema(builder, tabLevel + 1);
        }
    }

   @Override
    void appendShowAll(StringBuilder builder, int tabLevel) {
        appendSpecific(builder, tabLevel);
        for (MetaDataBase child:childMetaData){
            child.appendShowAll(builder, tabLevel + 1);
        }
    }

    @Override
    void appendUnusedStatements(StringBuilder builder) {
        for (MetaDataBase child:childMetaData){
            child.appendUnusedStatements(builder);
        }
    }

    // ** Abstract methods used above
    abstract void appendSpecific(StringBuilder builder, int tabLevel);
    
    /* Methods with no shared implementation 
    @Override
    MetaDataBase getSchemaClone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    void appendValidityReport(StringBuilder builder, RequirementLevel forceLevel, boolean includeWarnings, int tabLevel) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    */

    void addChildren(List<MetaDataBase> childMetaData) {
        this.childMetaData.addAll(childMetaData);
    }

}
