package geodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class GeoDBTest extends GeoDBTestSupport {
    /**
     * The name of the database.
     * @return the database name.
     */
    public static String getDatabaseName() {
        return "geodb";
    }

    /**
     * Creates the <code>InitGeoDB</code> procedure.
     * 
     * @param st
     *            the statement to execute the SQL.
     * @throws SQLException
     *             if unable to execute the SQL.
     */
    protected abstract void createInitGeoDBProcedure(Statement st) throws SQLException;

    @Before
    public void setUp() throws Exception {
        final Connection cx = getConnection();
        Statement st = cx.createStatement();
        String tableName = GeoDB.getGeoDBTableName(cx);
        dropTable(st, tableName);
        dropTable(st, "spatial");
        dropTable(st, "spatial_hatbox");
        dropTable(st, "noindex");
        st.close();
    }

    @Test
    public void testInitDB() throws Exception {
        final Connection cx = getConnection();
        ResultSet tables = cx.getMetaData().getTables(null, null, GeoDB.getGeoDBTableName(cx), new String[] {"TABLE"});
        assertFalse(tables.next());

        Statement st = cx.createStatement();
        createInitGeoDBProcedure(st);
        st.execute("CALL InitGeoDB()");
        st.close();
        tables = cx.getMetaData().getTables(null, null, GeoDB.getGeoDBTableName(cx), new String[] {"TABLE"});
        assertTrue(tables.next());
        
        tables.close();
    }

    @Test
    public void testCreateSpatialIndex() throws Exception {
        final Connection cx = getConnection();
        Statement st = cx.createStatement();

        createInitGeoDBProcedure(st);
        st.execute("CALL InitGeoDB()");

        createTable(st, "spatial", "id", "geom");
        st.execute("INSERT INTO spatial (geom) VALUES (ST_GeomFromText('POINT(0 0)', 4326))");
        st.execute("INSERT INTO spatial (geom) VALUES (ST_GeomFromText('POINT(1 1)', 4326))");
        st.execute("INSERT INTO spatial (geom) VALUES (ST_GeomFromText('POINT(2 2)', 4326))");
        
        ResultSet tables = 
            cx.getMetaData().getTables(null, null, "SPATIAL_HATBOX", new String[] {"TABLE"});
        assertFalse(tables.next());
        st.execute("CALL CreateSpatialIndex(null, 'SPATIAL', 'GEOM', '4326')");
        
        tables = 
            cx.getMetaData().getTables(null, null, "SPATIAL_HATBOX", new String[] {"TABLE"});
        assertTrue(tables.next());
        st.close();
    }

    @Test
    public void testDropSpatialIndex() throws Exception {
        final Connection cx = getConnection();
        Statement st = cx.createStatement();

        createInitGeoDBProcedure(st);
        st.execute("CALL InitGeoDB()");

        createTable(st, "spatial", "id", "geom");
        
        ResultSet tables = 
            cx.getMetaData().getTables(null, null, "SPATIAL_HATBOX", new String[] {"TABLE"});
        assertFalse(tables.next());
        st.execute("CALL CreateSpatialIndex(null, 'SPATIAL', 'GEOM', '4326')");
        
        tables = 
            cx.getMetaData().getTables(null, null, "SPATIAL_HATBOX", new String[] {"TABLE"});
        assertTrue(tables.next());

        st.execute("CALL DropSpatialIndex(null, 'SPATIAL')");
        tables = 
                cx.getMetaData().getTables(null, null, "SPATIAL_HATBOX", new String[] {"TABLE"});
        assertFalse(tables.next());

        st.close();
    }

    @Test
    public void testGetSRID() throws Exception {
        Statement st = getConnection().createStatement();
        createTable(st, "spatial", "id", "geom");
        st.execute("CALL CreateSpatialIndex(null, 'SPATIAL', 'GEOM', '4326')");
        assertEquals(4326, GeoDB.GetSRID(getConnection(), null, "SPATIAL"));
        
        createTable(st, "noindex", "id", "geom");
        assertEquals(-1, GeoDB.GetSRID(getConnection(), null, "NOINDEX"));
        
        st.close();
    }
}
