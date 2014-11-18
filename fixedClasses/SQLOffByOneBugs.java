import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class SQLOffByOneBugs {

    public void checkUpdateThis(ResultSet rs) throws SQLException {
        System.out.println(rs.getString(1));
    }
    
    public int checkUpdateThis2(ResultSet rs) throws SQLException {
        return rs.getInt(1);
    }
    
    public List<Object> checkUpdateAll(ResultSet rs) throws SQLException {
        List<Object> objects = new ArrayList<>();
        objects.add(rs.getString(1));
        objects.add(rs.getString(2));
        objects.add(rs.getInt(3));
        objects.add(rs.getObject(4));
        return objects;
    }
    
    public List<Object> checkUpdateAllMixedString(ResultSet rs) throws SQLException {
        List<Object> objects = new ArrayList<>();
        objects.add(rs.getString(1));
        objects.add(rs.getString(2));
        objects.add(rs.getInt("some_col"));
        objects.add(rs.getObject("other_col"));
        return objects;
    }
}
