package org.bridgedb.ws;

import java.io.File;
import java.net.MalformedURLException;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperCapabilities;
import org.bridgedb.IDMapperCapabilitiesTest;
import org.bridgedb.IDMapperException;
import org.bridgedb.file.IDMapperText;
import org.junit.BeforeClass;

/**
 * This test uses WSMapper directly for the Capabilities rather than downloading the whole capabilities xml.
 * @author Christian
 */
public class WSIDMapperAsCapabilitiesTest extends IDMapperCapabilitiesTest{
    
    @BeforeClass
    public static void setupIDMapper() throws IDMapperException, MalformedURLException{
        File INTERFACE_TEST_FILE = new File ("../org.bridgedb/test-data/interfaceTest.txt");
        IDMapper inner = new IDMapperText(INTERFACE_TEST_FILE.toURL());
        WSService wsService = new WSService(inner);
        idMapper = new WSMapper(wsService){
            public IDMapperCapabilities getCapabilities() {
                return this;
            }
        };
    }

}