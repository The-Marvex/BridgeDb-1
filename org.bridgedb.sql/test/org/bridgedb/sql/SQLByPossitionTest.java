/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bridgedb.sql;

import org.bridgedb.ws.XrefByPossitionTest;
import org.bridgedb.IDMapperException;
import org.junit.BeforeClass;

/**
 * Warning: These tests depend on the data loaded in the IDMapperSQLTest.
 * So the first time this is run (or run after base tests change) this these test may cause errors.
 * Once IDMapperSQLTest is run once these should be fine until the test data changes again.
 * @author Christian
 */
public class SQLByPossitionTest extends XrefByPossitionTest {
    
    @BeforeClass
    public static void setupIDMapper() throws IDMapperException{
        SQLAccess sqlAccess = TestSqlFactory.createTestSQLAccess();
        IDMapperSQL iDMapperSQL = new IDMapperSQL(sqlAccess);
        xrefByPossition = iDMapperSQL;
    }
     
}