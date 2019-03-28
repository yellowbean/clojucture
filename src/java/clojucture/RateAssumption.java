package clojucture;

import clojucture.DoubleFlow;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.api.Row;
import tech.tablesaw.columns.AbstractColumn;
import tech.tablesaw.joining.DataFrameJoiner;
import tech.tablesaw.selection.Selection;

import java.time.LocalDate;
import static java.time.temporal.ChronoUnit.DAYS;
import java.util.Arrays;

import static java.lang.Math.max;
import static java.lang.Math.min;


public class RateAssumption extends Table{

    public RateAssumption(String name, LocalDate[] s, LocalDate[] e, double[] o){

       super(name);

       //this.setName(name);

       DateColumn sd = DateColumn.create("Start",s);
       DateColumn ed = DateColumn.create("End",e);
       DoubleColumn v =DoubleColumn.create("Rate",o);
       this.addColumns(sd,ed,v);
    }

    public Double[] project(LocalDate[] d){

        Integer f = 0;
        LocalDate fdate = d[f];
        LocalDate ldate = d[d.length - 1];

        Selection sb = ((DateColumn) this.column("End")).isOnOrAfter(fdate);
        Selection se = ((DateColumn) this.column("Start")).isOnOrBefore(ldate);

        Table ra = this.where(sb.and(se));

        //return ra.rowCount();

        Double[] result_rate = new Double[d.length];
        Arrays.fill(result_rate,0.0);

        for(int k=0;k<ra.rowCount();k++){
            LocalDate sdate = ((DateColumn)ra.column("Start")).get(k);
            LocalDate edate = ((DateColumn)ra.column("End")).get(k);
            Double cv = ((DoubleColumn)ra.column("Rate")).get(k);
            while ( d[f].isAfter(sdate) && d[f].isBefore(edate) && (f < d.length)){
                result_rate[f] = cv;
                f++;
                if (f == d.length) break;
            }
        }
        return result_rate;

    }
}
