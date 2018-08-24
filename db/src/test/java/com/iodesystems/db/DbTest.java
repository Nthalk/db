package com.iodesystems.db;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.iodesystems.db.data.DataSet;
import com.iodesystems.db.data.Record;
import com.iodesystems.db.data.RecordCursor;
import com.iodesystems.db.errors.RollbackException;
import com.iodesystems.db.query.ColumnConfig;
import com.iodesystems.db.query.Ordering;
import com.iodesystems.db.query.Sort;
import com.iodesystems.db.search.Search;
import com.iodesystems.db.search.SearchContext;
import com.iodesystems.db.search.SearchParser;
import com.iodesystems.db.search.SearchTermGroup;
import java.math.BigDecimal;
import java.util.List;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.BeforeClass;
import org.junit.Test;

public class DbTest {

  static DataSource dataSource;
  static Db db;

  @BeforeClass
  public static void beforeClass() {
    dataSource = new JdbcDataSource();
    ((JdbcDataSource) dataSource).setURL("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1");
    db = new Db(dataSource);
    db.execute("CREATE TABLE T1 (NAME VARCHAR, VALUE NUMBER, EXTRA TEXT)");
    db.execute(
        "INSERT INTO T1 (NAME,VALUE,EXTRA) VALUES (:name, :number1, :e1), (:name, :number2, :e2),('BLERB', 3, :e2)",
        (p) -> p.set("name", "Namie").set("number1", 1).set("number2", 2));
  }

  @Test
  public void testSearchSetInternalParam() {
    assertEquals(0, db
        .query(""
            + "SELECT * FROM T1 WHERE :O = 1", s -> s
            .set("O", 1)
            .param("O")
        )
        .search("O:0")
        .getItems()
        .size());
  }

  @Test
  public void testCount() {
    assertEquals(3, db.query("SELECT * FROM T1").count());
  }

  @Test
  public void testSimplifiedColumn() {
    db.query("SELECT * FROM T1", q -> q.searchColumn("NAME", Search.STARTS_WITH));
  }

  @Test
  public void testSearchOr() {
    DataSet<Record> searchableDataSet =
        db.query("" +
            "SELECT * FROM T1 WHERE :O = 1", s -> s
            .column("NAME", c -> c.search((b) -> b.field() + "  = " + b.param()))
            .column("VALUE", c -> c.search((b) -> b.field() + " = " + b.param())));
    SearchContext searchContext =
        searchableDataSet
            .search("name:BLERB, VALUE:1 a b c name:BLERB")
            .search("VALUE:1 a b")
            .getSearchContext();
    assertEquals(
        "SELECT * FROM (\n"
            + "\n"
            + "SELECT * FROM T1 WHERE :O = 1\n"
            + "\n"
            + ") WHERE (    (    (       \"NAME\"  = :_NAME0 ))\n"
            + "      OR (    (       \"VALUE\" = :_VALUE1 )\n"
            + "               AND (    ( \"NAME\"  = :_NAME2 )\n"
            + "                     OR ( \"VALUE\" = :_VALUE3 ))\n"
            + "               AND (    ( \"NAME\"  = :_NAME4 )\n"
            + "                     OR ( \"VALUE\" = :_VALUE5 ))\n"
            + "               AND (    ( \"NAME\"  = :_NAME6 )\n"
            + "                     OR ( \"VALUE\" = :_VALUE7 ))\n"
            + "                    AND ( \"NAME\"  = :_NAME8 )))\n"
            + "    AND (    (    (       \"VALUE\" = :_VALUE9 )\n"
            + "               AND (    ( \"NAME\"  = :_NAME10 )\n"
            + "                     OR ( \"VALUE\" = :_VALUE11 ))\n"
            + "               AND (    ( \"NAME\"  = :_NAME12 )\n"
            + "                     OR ( \"VALUE\" = :_VALUE13 ))))",
        searchContext.getSql());
  }

  @Test
  public void testMap() {
    DataSet<Record> map =
        db.query("" + "SELECT * FROM T1", s -> s.column("VALUE", ColumnConfig::orderable))
            .order("VALUE", Sort.DESC)
            .map(r -> r);
  }

  @Test
  public void testOrdering() {
    assertArrayEquals(
        new Integer[]{3, 2, 1},
        db.query("" + "SELECT * FROM T1", s -> s.column("VALUE", ColumnConfig::orderable))
            .order("VALUE", Sort.DESC)
            .load(this::testItem)
            .map(i -> i.getValue().intValue())
            .getItems()
            .toArray());

    assertArrayEquals(
        new Integer[]{1, 2, 3},
        db.query("" + "SELECT * FROM T1", s -> s.column("VALUE", ColumnConfig::orderable))
            .order("VALUE", Sort.ASC)
            .load(this::testItem)
            .map(i -> i.getValue().intValue())
            .getItems()
            .toArray());
  }

  @Test
  public void testParser() {
    SearchParser searchParser = new SearchParser();
    List<SearchTermGroup> parse = searchParser.parse(" asdf ");
    assertEquals(1, parse.size());

    List<SearchTermGroup> parse2 = searchParser.parse(" asdf eee ");
    assertEquals(1, parse2.size());
  }

  @Test
  public void testParser2() {
    List<SearchTermGroup> parse = new SearchParser().parse(",");
    assertEquals(0, parse.size());
  }

  @Test
  public void testParser3() {
    List<SearchTermGroup> parse = new SearchParser().parse(" A , B C ");
    assertEquals(2, parse.size());
    assertEquals(1, parse.get(0).getTerms().size());
    assertEquals(2, parse.get(1).getTerms().size());
  }

  @Test
  public void testSearchContext() {
    DataSet<Record> searchableDataSet =
        db.query("" +
            "SELECT * FROM T1 WHERE :O = 1", s -> s
            .set("O", 1)
            .param("O")
            .column("NAME", c -> c.search((b) -> b.field() + "=" + b.param())));
    assertEquals(2, searchableDataSet.search("Namie").getItems().size());
  }

  @Test
  public void testSearch() {
    DataSet<Record> searchableDataSet =
        db.query("" +
            "SELECT * FROM T1 WHERE :O = 1", s -> s
            .set("O", 1)
            .param("O")
            .column("NAME", c -> c.search((b) -> b.field() + "=" + b.param())));

    assertEquals(3, searchableDataSet.getItems().size());
    assertEquals(0, searchableDataSet.search("DERP").getItems().size());
    assertEquals(2, searchableDataSet.search("Namie").getItems().size());
    assertEquals(1, searchableDataSet.search("BLERB").getItems().size());
    assertEquals(0, searchableDataSet.search("O:0").getItems().size());
  }

  @Test
  public void testTransactions() {
    testTransactionRunner();
    testTransactionRunner();
  }

  private void testTransactionRunner() {
    long count = db.query("SELECT * FROM T1").count();
    int[] mustCall = new int[2];
    try {
      db.transaction(tDb -> {
        tDb.execute("INSERT INTO T1 () VALUES ()");
        assertEquals(count + 1, tDb.query("SELECT * FROM T1").count());
        mustCall[0] = 1;
        throw new RollbackException();
      });
    } catch (RollbackException e) {
      mustCall[1] = 1;
    }
    assertEquals(count, db.query("SELECT * FROM T1").count());
    assertEquals(1, mustCall[0]);
    assertEquals(1, mustCall[1]);
  }

  @Test
  public void testFirst() {
    String first = db.query("SELECT * FROM T1").first(String.class);
    assertEquals("Namie", first);
  }

  @Test
  public void test() {
    DataSet<Record> simpleRecords = db.query("SELECT * FROM T1");
    List<Record> simpleRecordItems = simpleRecords.getItems();
    DataSet<Record> page = simpleRecords.page(0, 2);
    List<Record> page1 = page.getItems();
    assertEquals(2, page1.size());
    assertEquals(BigDecimal.ONE, page1.get(0).getBigDecimal("VALUE"));
    assertEquals(new BigDecimal(2), page1.get(1).getBigDecimal("VALUE"));
    List<Record> page2 = page.nextPage().getItems();
    assertEquals(2, page1.size());
    assertEquals(new BigDecimal(3), page2.get(0).getBigDecimal("VALUE"));

    DataSet<TestItem> mappedDataSet = db.query("SELECT * FROM T1").load(this::testItem);
    List<TestItem> mappedItems = mappedDataSet.getItems();
    assertEquals(3, mappedItems.size());
    assertEquals(new BigDecimal(1), mappedItems.get(0).getValue());
    assertEquals(new BigDecimal(2), mappedItems.get(1).getValue());
    assertEquals(new BigDecimal(3), mappedItems.get(2).getValue());

    DataSet<Integer> query =
        db.query(""
            + "SELECT 3 VALUE FROM DUAL WHERE :V = 1", q -> q
            .set("V", 1)
            .param("S")
            .column(
                "VALUE", c -> c.search(b -> b.field() + " = " + b.param()).orderable()))
            .load(r -> 1);
    DataSet<Integer> query2 = query.search("eee");
    DataSet<Integer> query3 = query.order(new Ordering("VALUE", Sort.DESC));
    DataSet<Integer> query4 = query.page(0, 50);
  }

  private TestItem testItem(RecordCursor r) {
    return new TestItem(r.getString("NAME"), r.getBigDecimal("VALUE"), r.getString("EXTRA"));
  }

  private class TestItem {

    private final String name;
    private final BigDecimal value;
    private final String extra;

    public TestItem(String name, BigDecimal value, String extra) {
      this.name = name;
      this.value = value;
      this.extra = extra;
    }

    public String getName() {
      return name;
    }

    public BigDecimal getValue() {
      return value;
    }

    public String getExtra() {
      return extra;
    }
  }
}
