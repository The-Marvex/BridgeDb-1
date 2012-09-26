// BridgeDb,
// An abstraction layer for identifier mapping services, both local and online.
//
// Copyright      2012  Christian Y. A. Brenninkmeijer
// Copyright      2012  OpenPhacts
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
package org.bridgedb.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperCapabilities;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.impl.InternalUtils;

/**
 * Builds on the SQLListener to implement the Standard BridgeDB functions of IDMapper and IDMapperCapabilities.
 *
 * This Allows the OPS version to function as any other BridgeDB implementation
 *
 * @author Christian
 */
public class SQLIdMapper extends SQLListener implements IDMapper, IDMapperCapabilities {

    /** 
     * FreeSearch has proven to be very slow over large database so for large database we say it is unsupported.
     */
    private static final int FREESEARCH_CUTOFF = 100000;      
    //Internal parameters
    protected static final int DEFAULT_LIMIT = 1000;
    /**
     * This identifies version of SQL such as MySQL that use "LIMIT" to restrict the number of tuples returned.
     */
    private final boolean useLimit;
    /**
     * This identifies version of SQL such as Virtuoso that use "TOP" to restrict the number of tuples returned.
     */
    private final boolean useTop;
    
    /**
     * Creates a new BridgeDB implementation based on a connection to the SQL Database.
     * 
     * @param dropTables Flag to dettermine if any existing tables should be dropped and new empty tables created.
     * @param sqlAccess The connection to the actual database. This could be MySQL, Virtuoso ect. 
     *       It could also be the live database, the loading database or the test database.
     * @param specific Code to hold the things that are different between different SQL implementaions.
     * @throws BridgeDbSqlException 
     */
    public SQLIdMapper(boolean dropTables, SQLAccess sqlAccess, SQLSpecific specific) throws BridgeDbSqlException{
        super(dropTables, sqlAccess, specific);
        useLimit = specific.supportsLimit();
        useTop = specific.supportsTop();
     }   

    //*** IDMapper Methods 
    
    @Override
    public Map<Xref, Set<Xref>> mapID(Collection<Xref> srcXrefs, DataSource... tgtDataSources) throws IDMapperException {
        return InternalUtils.mapMultiFromSingle(this, srcXrefs, tgtDataSources);
    }

    @Override
    public Set<Xref> mapID(Xref ref, DataSource... tgtDataSources) throws BridgeDbSqlException {
        if (badXref(ref)) return new HashSet<Xref>();
        StringBuilder query = new StringBuilder();
        query.append("SELECT targetId as id, targetDataSource as sysCode ");
        query.append("FROM mapping, mappingSet ");
        query.append("WHERE mappingSetId = mappingSet.id ");
        appendSourceXref(query, ref);
        if (tgtDataSources.length > 0){    
            query.append("AND ( targetDataSource = '");
                query.append(tgtDataSources[0].getSystemCode());
                query.append("' ");
            for (int i = 1; i < tgtDataSources.length; i++){
                query.append("OR targetDataSource = '");
                    query.append(tgtDataSources[i].getSystemCode());
                    query.append("'");
            }
            query.append(")");
        }
        //ystem.out.println(query);
        Statement statement = this.createStatement();
        ResultSet rs;
        try {
            rs = statement.executeQuery(query.toString());
        } catch (SQLException ex) {
            throw new BridgeDbSqlException("Unable to run query. " + query, ex);
        }    
        Set<Xref> results = resultSetToXrefSet(rs);
        if (tgtDataSources.length == 0){
           results.add(ref); 
        } else {
            for (DataSource tgtDataSource: tgtDataSources){
                if (ref.getDataSource().equals(tgtDataSource)){
                    results.add(ref);
                }
            }
        }
        return results;
    }

    @Override
    public boolean xrefExists(Xref xref) throws BridgeDbSqlException {
        if (badXref(xref)) return false;
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        appendTopConditions(query, 0, 1); 
        query.append("targetId ");
        query.append("FROM mapping, mappingSet ");
        query.append("WHERE mappingSetId = mappingSet.id ");
        appendSourceXref(query, xref);
        appendLimitConditions(query,0, 1);
        Statement statement = this.createStatement();
        ResultSet rs;
        try {
            rs = statement.executeQuery(query.toString());
            return rs.next();
        } catch (SQLException ex) {
            throw new BridgeDbSqlException("Unable to run query. " + query, ex);
        }    
   }

    @Override
    public Set<Xref> freeSearch(String text, int limit) throws BridgeDbSqlException {
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        appendTopConditions(query, 0, limit); 
        query.append(" targetId as id, targetDataSource as sysCode ");
        query.append("FROM mapping, mappingSet ");
        query.append("WHERE mappingSetId = mappingSet.id ");
        query.append("AND sourceId = '");
            query.append(text);
            query.append("' ");
        appendLimitConditions(query,0, limit);
        Statement statement = this.createStatement();
        ResultSet rs;
        try {
            rs = statement.executeQuery(query.toString());
        } catch (SQLException ex) {
            throw new BridgeDbSqlException("Unable to run query. " + query, ex);
        }    
        return resultSetToXrefSet(rs);
    }

    @Override
    public IDMapperCapabilities getCapabilities() {
        return this;
    }

    //BridgeDB expects that once close is called isConnected will return false
    private boolean isConnected = true;
    
    @Override
    /** {@inheritDoc} */
    public void close() throws BridgeDbSqlException { 
        isConnected = false;
        if (this.possibleOpenConnection != null){
            try {
                this.possibleOpenConnection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    /** {@inheritDoc} */
    public boolean isConnected() { 
        if (isConnected){
            try {
                sqlAccess.getConnection();
                return true;
            } catch (BridgeDbSqlException ex) {
                return false;
            }
        }
        return isConnected; 
    }
    // ***IDMapperCapabilities
    
    @Override
    public boolean isFreeSearchSupported() {
        return true;
    }

    @Override
    public Set<DataSource> getSupportedSrcDataSources() throws BridgeDbSqlException {
        StringBuilder query = new StringBuilder();
        query.append("SELECT sourceDataSource as sysCode ");
        query.append("FROM mappingSet ");
        Statement statement = this.createStatement();
        ResultSet rs;
        try {
            rs = statement.executeQuery(query.toString());
        } catch (SQLException ex) {
            throw new BridgeDbSqlException("Unable to run query. " + query, ex);
        }    
        return resultSetToDataSourceSet(rs);        
    }

    @Override
    public Set<DataSource> getSupportedTgtDataSources() throws BridgeDbSqlException {
        StringBuilder query = new StringBuilder();
        query.append("SELECT targetDataSource as sysCode ");
        query.append("FROM mappingSet ");
        Statement statement = this.createStatement();
        ResultSet rs;
        try {
            rs = statement.executeQuery(query.toString());
        } catch (SQLException ex) {
            throw new BridgeDbSqlException("Unable to run query. " + query, ex);
        }    
        return resultSetToDataSourceSet(rs);        
    }

    @Override
    public boolean isMappingSupported(DataSource src, DataSource tgt) throws BridgeDbSqlException {
        StringBuilder query = new StringBuilder();
        query.append("SELECT predicate ");
        query.append("FROM mappingSet ");
        query.append("WHERE sourceDataSource = '");
            query.append(src.getSystemCode());
            query.append("' ");        
        query.append("AND targetDataSource = '");
            query.append(tgt.getSystemCode());
            query.append("' ");        
        
        Statement statement = this.createStatement();
        ResultSet rs;
        try {
            rs = statement.executeQuery(query.toString());
            return rs.next();
        } catch (SQLException ex) {
            throw new BridgeDbSqlException("Unable to run query. " + query, ex);
        }    
    }

    @Override
    public String getProperty(String key) {
        String query = "SELECT DISTINCT property "
                + "FROM properties "
                + "WHERE theKey = '" + key + "'";
        try {
            Statement statement = this.createStatement();
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()){
                return rs.getString("property");
            } else {
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Set<String> getKeys() {
        HashSet<String> results = new HashSet<String>();
        String query = "SELECT theKey "
                + "FROM properties "
                + "WHERE isPublic = 1"; //one works where isPublic is a boolean
        try {
            Statement statement = this.createStatement();
            ResultSet rs = statement.executeQuery(query);
            while (rs.next()){
                results.add(rs.getString("theKey"));
            }
            return results;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    //**** Support methods 
    
    /**
     * Check if the Xref is invalid in some way.
     * <p>
     * For example if it is null or if either the Id or DataSource part is null. 
     * @param ref
     * @return 
     */
    private final boolean badXref(Xref ref) {
        if (ref == null) return true;
        if (ref.getId() == null || ref.getId().isEmpty()) return true;
        if (ref.getDataSource() == null ) return true;
        return false;
    }

    /**
     * Add a condition to the query that only mappings with a specific source Xref should be used.
     * @param query Query to add to.
     * @param ref Xref that forms the base of the condition.
     */
    private final void appendSourceXref(StringBuilder query, Xref ref){
        query.append("AND sourceId = '");
            query.append(ref.getId());
            query.append("' ");
       query.append("AND sourceDataSource = '");
            query.append(ref.getDataSource().getSystemCode());
            query.append("' ");        
    }
    
    /**
     * Converts a ResultSet to a Set of individual Xrefs.
     * @throws BridgeDbSqlException 
     */
    private Set<Xref> resultSetToXrefSet(ResultSet rs) throws BridgeDbSqlException {
        HashSet<Xref> results = new HashSet<Xref>();
        try {
            while (rs.next()){
                String id = rs.getString("id");
                String sysCode = rs.getString("sysCode");
                DataSource dataSource = DataSource.getBySystemCode(sysCode);
                Xref xref = new Xref(id, dataSource);
                results.add(xref);
            }
            return results;
       } catch (SQLException ex) {
            throw new BridgeDbSqlException("Unable to parse results.", ex);
       }
    }

    /**
     * Converts a ResultSet to a Set of BridgeDB DataSources by obtaining the SysCode from the ResultsSet a
     * and looking the DataSource up in the DataSource Registry.
     * <p>
     * All DataSources have been preloaded by loadDataSources() called during the super constructor.
     * @param rs
     * @return
     * @throws BridgeDbSqlException 
     */
    private Set<DataSource> resultSetToDataSourceSet(ResultSet rs) throws BridgeDbSqlException {
        HashSet<DataSource> results = new HashSet<DataSource>();
        try {
            while (rs.next()){
                String sysCode = rs.getString("sysCode");
                DataSource dataSource = DataSource.getBySystemCode(sysCode);
                results.add(dataSource);
            }
            return results;
       } catch (SQLException ex) {
            throw new BridgeDbSqlException("Unable to parse results.", ex);
       }
    }

    /**
     * Adds the limit condition if this is the support method for limitng the number of results.
     * <p>
     * System such as MYSQL use a Limit clause at the end of the query. Where applicable this is added here.
     * @param query Query to add Limit to
     * @param position The offset of the fragment of results to return
     * @param limit The size of the fragment of results to return
     */
    protected void appendLimitConditions(StringBuilder query, Integer position, Integer limit){
        if (useLimit){
            if (position == null) {
                position = 0;
            }
            if (limit == null){
                limit = DEFAULT_LIMIT;
            }
            query.append("LIMIT " + position + ", " + limit);       
        }
    }

    /**
     * Adds the top condition if this is the support method for limitng the number of results.
     * <p>
     * System such as Virtuosos use a Top clause directly after the select. Where applicable this is added here.
     * @param query Query to add TOP to
     * @param position The offset of the fragment of results to return
     * @param limit The size of the fragment of results to return
     */
    protected void appendTopConditions(StringBuilder query, Integer position, Integer limit){
        if (useTop){
            if (position == null) {
                position = 0;
            }
            if (limit == null){
                limit = DEFAULT_LIMIT;
            }
            query.append("TOP " + position + ", " + limit + " ");                
        }
    }



}