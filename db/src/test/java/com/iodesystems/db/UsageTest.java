package com.iodesystems.db;

import static org.junit.Assert.assertEquals;

import com.iodesystems.db.data.DataSet;
import com.iodesystems.db.data.Page;
import com.iodesystems.db.data.ParamaterizedStatement;
import com.iodesystems.db.data.Query;
import com.iodesystems.db.data.TypedDataSet;
import com.iodesystems.db.query.Column;
import com.iodesystems.db.query.Ordering;
import com.iodesystems.db.query.Sort;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;

public class UsageTest {

  @Test
  public void testUsage() {
    Db db = new Db(dataSource());

    db.execute("DELETE FROM MY_TABLE");

    DataSet dataSet = db.query("SELECT * FROM MY_TABLE");
    assertEquals(0, dataSet.count());

    /**
     * Query execution with bind parameters
     */
    db.execute(""
        + "INSERT INTO MY_TABLE "
        + "(NAME, VALUE, EXTRA) "
        + "VALUES "
        + "(:name, :value, :extra)", p -> p
        .set("name", "Carl")
        .set("value", 100)
        .set("extra", "Some notes!"));
    assertEquals(1, dataSet.count());

    /**
     * Sometimes you need to loop over a statement to insert data
     */
    try (ParamaterizedStatement prepare = db.prepare(""
        + "INSERT INTO MY_TABLE "
        + "(NAME, VALUE, EXTRA) "
        + "VALUES "
        + "(:name, :value, :extra)")) {

      int insertCount = prepare.execute(p -> p
          .set("name", "Steve")
          .set("value", 3)
          .set("extra", null));
      assertEquals(1, insertCount);

      int updated = prepare.add(p -> p
          .set("name", "Dessi")
          .set("value", 30000)
          .set("extra", "Doggo"))
          .execute();
      assertEquals(1, updated);
    }

    /**
     * Searchable, sortable, parameterized query with a transformed DataSet
     */
    TypedDataSet<String> searchableDataSet = db
        .query("SELECT * FROM MY_TABLE WHERE :INNER_PARAM = 0", queryBuilder -> queryBuilder
            // Build a column
            .column("NAME", columnBuilder -> columnBuilder
                // With a search condition generator
                .search(search ->
                    search.field() + " ilike " + search.param("%" + search.term() + "%"))
                // Allow this column to be ordered
                .orderable())
            // Allow INNER_PARAM to be specified
            .param("INNER_PARAM")
            // Set a default
            .set("INNER_PARAM", 0))
        .map(recordCursor -> recordCursor.getString("NAME"));

    assertEquals(Arrays.asList("Carl", "Steve", "Dessi"), searchableDataSet.getItems());
    assertEquals(Arrays.asList("Dessi"), searchableDataSet.search("name:Dessi").getItems());
    assertEquals(Arrays.asList("Carl", "Dessi"),
        searchableDataSet.search("name:Dessi, name:Carl").getItems());

    /**
     * Searches are sub searchable:
     */
    assertEquals(Arrays.asList("Steve", "Dessi"), searchableDataSet.search("name:e").getItems());
    assertEquals(Arrays.asList("Dessi"),
        searchableDataSet.search("name:e").search("name:i").getItems());

    /**
     * DataSets are pageable
     */
    TypedDataSet<String> page = searchableDataSet.page(0, 2);
    assertEquals(Arrays.asList("Carl", "Steve"), page.getItems());
    assertEquals(Arrays.asList("Dessi"), page.nextPage().getItems());

    /**
     * You can retrieve column info about a DataSet
     */
    List<Column<?>> columns = searchableDataSet.getUnderlyingColumns();
    assertEquals(3, columns.size());
    assertEquals("NAME", columns.get(0).getName());
    assertEquals("VALUE", columns.get(1).getName());
    assertEquals("EXTRA", columns.get(2).getName());

    /**
     * A simple query object can be marshalled via JSON or built and used to retrieve a serializable
     * Page
     */
    Query query = new Query();
    query.setSearch("e");
    query.setPage(0);
    query.setPageSize(2);
    query.setOrderings(Arrays.asList(new Ordering("name", Sort.DESC)));

    Page<String> simplePage = searchableDataSet.query(query);
    List<String> items = simplePage.getItems();

  }

  private DataSource dataSource() {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1");
    Db db = new Db(dataSource);
    db.execute("CREATE TABLE MY_TABLE (NAME VARCHAR, VALUE NUMBER, EXTRA TEXT)");
    return dataSource;
  }

}
