package clojucture;

import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.aggregate.Summarizer;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.joining.DataFrameJoiner;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Cashflow extends Table {

    public Cashflow(Table t){
        super(t.name());
        for(int i = 0; i<t.columnCount();i++){
            this.addColumns(t.column(i));
        }

        String[] c2t = new String[]{"dates"};

        this.setName("CF");
        this.trim_sum(c2t);
        this.sortAscendingOn("dates");
    }

    public Cashflow(String name){
        super(name);
    }

    public Cashflow(String name, LocalDate[] d){
        super(name);
        DateColumn dts = DateColumn.create("DATES", d);
        this.addColumns(dts);
    }

    public Cashflow aggregateByInterval(String name, LocalDate[] d){

        Cashflow AggregatedCashflow = new Cashflow(name);
        LocalDate[] endDates = Arrays.copyOfRange(d, 1, d.length);
        LocalDate[] startDates = Arrays.copyOfRange(d, 0, d.length-1);

        DateColumn startDate = DateColumn.create("Start Date", startDates);
        DateColumn endDate = DateColumn.create("End Date", endDates);


        return AggregatedCashflow;
    }


    public Cashflow add( Cashflow cf){
        Table combined_cf = this.append(cf);
        final List<String> agg_exl_field = Arrays.asList("dates","balance");
        List<String> all_column_names = this.columnNames().stream().filter( e -> !agg_exl_field.contains(e)).collect(Collectors.toList());
        Summarizer smr = combined_cf.summarize( all_column_names , AggregateFunctions.sum);

        Table agg_cf = smr.by("dates");

            agg_cf.column("Sum [principal]").setName("principal");
            agg_cf.column("Sum [default]").setName("default");
            agg_cf.column("Sum [prepayment]").setName("prepayment");
            agg_cf.column("Sum [interest]").setName("interest");

        Double init_balance = (Double)this.column("balance").get(0) + (Double)cf.column("balance").get(0);
        Double[] bal_array = new Double[agg_cf.rowCount()];
        bal_array[0] = init_balance;

        for(int i = 1 ;i<agg_cf.rowCount();i++){
            bal_array[i] = bal_array[i-1] - (Double)agg_cf.column("principal").get(i) - (Double)agg_cf.column("default").get(i) - (Double)agg_cf.column("prepayment").get(i);
        }
        DoubleColumn bal_col = DoubleColumn.create("balance", bal_array);

        return new Cashflow(agg_cf.addColumns(bal_col));

    }

    public void trim_sum( String[] exclude_column_names ){
        String p = ".*\\[(\\S+)\\].*";
        Pattern pp = Pattern.compile(p);

        for(int i = 0;i<this.columnCount();i++){
            String c_name = this.column(i).name();

            if (Arrays.stream(exclude_column_names).anyMatch(c_name::equals))
                continue;
            Matcher m = pp.matcher(c_name);
            if (m.find()){
                this.column(i).setName(m.group(1));
            }
        }
    }

    public Cashflow insertDates( LocalDate [] d){
        Cashflow dcf =  new Cashflow("dates cashflow", d);

        DataFrameJoiner dfj = new DataFrameJoiner(this, "DATES");

        return new Cashflow(dfj.fullOuter(dcf));
    }

}
