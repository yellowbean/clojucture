package clojucture;

import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.AbstractColumn;
import tech.tablesaw.columns.Column;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
/*
public class PoolCollection extends Table {
   public PoolCollection(String name, LocalDate[] interval){
       super(name);

       LocalDate[] startDates = Arrays.copyOfRange(interval, 0, interval.length-1);
       LocalDate[] endDates = Arrays.copyOfRange(interval, 1, interval.length);

       DateColumn startDate = DateColumn.create("Start", startDates);
       DateColumn endDate = DateColumn.create("End", endDates);

       this.addColumns(startDate,endDate);

   }

   public void collect(Cashflow cf){
       Row idx_row = new Row(this);
       Row idx_cf_row = new Row(cf);

       List<DoubleColumn> cf_col = cf.emptyCopy().columns();
       cf_col.removeIf(c -> c.name() == "Balance");
       this.addColumns(cf_col);

       while(idx_row.hasNext()){
           LocalDate cf_date = idx_cf_row.getDate("Date");
           if ( cf_date.isAfter(idx_row.getDate("Start")) && cf_date.isBefore(idx_row.getDate("End"))){

           }

           idx_row.next();
       }

   }

}

 */
