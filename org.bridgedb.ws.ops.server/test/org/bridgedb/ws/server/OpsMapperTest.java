/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bridgedb.ws.server;

import java.net.MalformedURLException;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.sql.SQLAccess;
import org.bridgedb.sql.TestSqlFactory;
import org.bridgedb.mysql.MysqlMapper;
import org.bridgedb.ops.LinkSetStore;
import org.bridgedb.ops.StubLinkSetStore;
import org.bridgedb.rdf.RdfReader;
import org.bridgedb.rdf.RdfStoreType;
import org.bridgedb.ws.WSCoreInterface;
import org.bridgedb.ws.WSCoreMapper;
import org.bridgedb.ws.WSCoreService;
import org.bridgedb.ws.WSInterface;
import org.bridgedb.ws.WSMapper;
import org.bridgedb.ws.WSService;
import org.junit.BeforeClass;

/**
 *
 * @author Christian
 */
public class OpsMapperTest extends org.bridgedb.ops.OpsMapperTest{
    
    @BeforeClass
    public static void setupIDMapper() throws IDMapperException, MalformedURLException{
        connectionOk = false;
        SQLAccess sqlAccess = TestSqlFactory.createTestSQLAccess();
        connectionOk = true;
        IDMapper inner = new MysqlMapper(sqlAccess);
        LinkSetStore linksetStore = new RdfReader(RdfStoreType.TEST);
        WSInterface webService = new WSService(inner, linksetStore);
        opsMapper = new WSMapper(webService);
    }

}
