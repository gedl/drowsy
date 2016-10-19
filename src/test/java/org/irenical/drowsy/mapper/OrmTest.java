package org.irenical.drowsy.mapper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.irenical.drowsy.PGTestUtils;
import org.irenical.drowsy.mapper.bean.LegitPerson;
import org.irenical.drowsy.mapper.bean.UnlegitPerson;
import org.junit.Test;

public class OrmTest extends PGTestUtils {

  @Test
  public void testOk()
      throws SQLException, IOException, InstantiationException, IllegalAccessException {
    Connection c = PGTestUtils.createConnection(true);

    PreparedStatement ps = c.prepareStatement("select * from people");
    ResultSet rs = ps.executeQuery();
    BeanMapper orm = new BeanMapper();

    List<LegitPerson> peeps = orm.map(rs, LegitPerson.class);
    System.out.println(peeps);
  }
  
  @Test(expected=SQLException.class)
  public void testReflectionException()
      throws SQLException, IOException, InstantiationException, IllegalAccessException {
    Connection c = PGTestUtils.createConnection(true);

    PreparedStatement ps = c.prepareStatement("select * from people");
    ResultSet rs = ps.executeQuery();
    BeanMapper orm = new BeanMapper();

    orm.map(rs, UnlegitPerson.class);
  }

}
