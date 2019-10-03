package clojucture;

import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.lang3.ArrayUtils;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.aggregate.Summarizer;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.AbstractColumn;
import tech.tablesaw.columns.Column;
import tech.tablesaw.joining.DataFrameJoiner;
import tech.tablesaw.table.TableSliceGroup;

import java.security.InvalidParameterException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Cashflow extends Table {

    public Cashflow(Table t) {
        super(t.name());
        for (int i = 0; i < t.columnCount(); i++) {
            this.addColumns(t.column(i));
        }
    }

    public Cashflow(String name) {
        super(name);
    }

    public Cashflow(String name, LocalDate[] d) {
        super(name);
        DateColumn dts = DateColumn.create("dates", d);
        this.addColumns(dts);
    }


    public LocalDate getStartDate() {
        return (LocalDate) this.column("dates").get(0);
    }

    public LocalDate getLastDate() {
        DateColumn dc = (DateColumn) this.column("dates");
        return dc.get(dc.size() - 1);
    }

    //public List<Table> aggregateByInterval(String name, LocalDate[] d, ){
    //}

    public Cashflow groupByInterval(String name, LocalDate[] d) {


        Cashflow new_table = new Cashflow(this);
        Cashflow AggregatedCashflow = new Cashflow(name);

        LocalDate cf_first_date = this.getStartDate();
        LocalDate cf_last_date = this.getLastDate();

        if (cf_first_date.isAfter(d[0])||cf_last_date.isBefore(d[d.length-1]))
            throw new InvalidParameterException("Seperate Dates should fall within date range of Cashflow");

        ArrayList<LocalDate> dl = new ArrayList<LocalDate>(Arrays.asList(d));

        dl.add(cf_first_date);
        dl.add(cf_last_date);
        Collections.sort(dl);

        LocalDate[] dxx = dl.toArray(new LocalDate[dl.size()]);

        //generate intervals : start dates
        //LocalDate[] startDates = Arrays.copyOfRange(dxx, 0, dxx.length - 1);

        //generate intervals : end dates
        //LocalDate[] endDates = Arrays.copyOfRange(dxx, 1, dxx.length);
        //List<LocalDate> adj_endDates = Arrays.asList(endDates).stream().map(x -> x.minusDays(1)).collect(Collectors.toList());
        //LocalDate[] adj_endDates_array = adj_endDates.toArray(new LocalDate[adj_endDates.size()]);

        //DateColumn startDate = DateColumn.create("Start Date", startDates);
        //DateColumn endDate = DateColumn.create("End Date", adj_endDates_array);

        //AggregatedCashflow.addColumns(startDate, endDate);

        DateColumn dc = (DateColumn) this.column("dates");

        Integer dates_flag = 0;
        LocalDate[] cfDates = dc.asObjectArray();
        Integer group_id[] = new Integer[cfDates.length];
        Arrays.fill(group_id, null);

        LocalDate sd;
        LocalDate ed;

        //filling group id to split the cashflow table by intervals
        for (int i = 0; i < dxx.length-1; i++) {
            sd = dxx[i];
            ed = dxx[i+1];
            do{
                group_id[dates_flag] = i;
                dates_flag++;

                if (dates_flag == group_id.length-1)
                    group_id[dates_flag] = i;

            } while (  (dates_flag<group_id.length) && (cfDates[dates_flag].isBefore(ed))&& cfDates[dates_flag].isAfter(sd) ) ;
        }

        IntColumn grp_index = IntColumn.create("Group Index", ArrayUtils.toPrimitive(group_id, -99));
        new_table.addColumns(grp_index);
        return new_table;
    }

    public List<Table>splitByGroup() {
        TableSliceGroup tsg = this.splitOn("Group Index");
        return tsg.asTableList();
    }


    protected List<String> getCashColumnNames(){
        List<String> r = this.columns()
                    .stream()
                    .filter(p -> p instanceof CashColumn)
                    .map(p -> p.name())
                    .collect(Collectors.toList());
        return r;
    }

    public Table aggByGroup(){
       List<String> col_to_agg = this.getCashColumnNames();
       Summarizer smr = new Summarizer(this, col_to_agg,AggregateFunctions.sum);
       return smr.by("Group Index");
    }

    /*
    public Cashflow add(Cashflow cf) {
        Table combined_cf = this.append(cf);
        final List<String> agg_exl_field = Arrays.asList("dates", "balance");
        List<String> all_column_names = this.columnNames().stream().filter(e -> !agg_exl_field.contains(e)).collect(Collectors.toList());
        Summarizer smr = combined_cf.summarize(all_column_names, AggregateFunctions.sum);

        Table agg_cf = smr.by("dates");

        agg_cf.column("Sum [principal]").setName("principal");
        agg_cf.column("Sum [default]").setName("default");
        agg_cf.column("Sum [prepayment]").setName("prepayment");
        agg_cf.column("Sum [interest]").setName("interest");

        Double init_balance = (Double) this.column("balance").get(0) + (Double) cf.column("balance").get(0);
        Double[] bal_array = new Double[agg_cf.rowCount()];
        bal_array[0] = init_balance;

        for (int i = 1; i < agg_cf.rowCount(); i++) {
            bal_array[i] = bal_array[i - 1] - (Double) agg_cf.column("principal").get(i) - (Double) agg_cf.column("default").get(i) - (Double) agg_cf.column("prepayment").get(i);
        }
        DoubleColumn bal_col = DoubleColumn.create("balance", bal_array);

        return new Cashflow(agg_cf.addColumns(bal_col));

    }*/

    public void trim_sum(String[] exclude_column_names) {
        String p = ".*\\[(\\S+)\\].*";
        Pattern pp = Pattern.compile(p);

        for (int i = 0; i < this.columnCount(); i++) {
            String c_name = this.column(i).name();

            if (Arrays.stream(exclude_column_names).anyMatch(c_name::equals))
                continue;
            Matcher m = pp.matcher(c_name);
            if (m.find()) {
                this.column(i).setName(m.group(1));
            }
        }
    }


}
