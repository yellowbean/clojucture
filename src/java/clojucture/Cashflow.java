package clojucture;

import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.lang3.ArrayUtils;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.aggregate.NumericAggregateFunction;
import tech.tablesaw.aggregate.Summarizer;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.AbstractColumn;
import tech.tablesaw.columns.Column;
import tech.tablesaw.joining.DataFrameJoiner;
import tech.tablesaw.table.TableSliceGroup;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Cashflow extends Table {

    public Cashflow(Table t) {
        super(t.name(), t.columnArray());
        //this.sortAscendingOn("dates");
    }

    public Cashflow(String name) {
        super(name);
    }

    public Cashflow(String name, LocalDate[] d) {
        super(name);
        DateColumn dts = DateColumn.create("dates", d);
        this.addColumns(dts);
    }

    public List<Table>splitByGroup() {
        TableSliceGroup tsg = this.splitOn("Group Index");
        return tsg.asTableList();
    }


    protected List<Column<?>> getCashColumn(){
        /*List<String> r = this.columns()
                    .stream()
                    .filter(p -> p instanceof CashColumn)
                    .map(p -> p.name())
                    .collect(Collectors.toList());

         */
        List<Column<?>> cn_list = this.columns();
        List<Column<?>> r_list =  new ArrayList<>();
        for( Column<?> cn : cn_list){
            if (cn instanceof CashColumn){
                r_list.add(cn);
            }
        }

        return r_list;
    }


    public Cashflow aggByDates(Cashflow cf) {
        Table combined_cf = this.append(cf);
        //final List<String> agg_exl_field = Arrays.asList("dates", "balance");
        List<Column<?>> sum_columns =  this.getCashColumn();
        //sum_column_names.add("balance");
        Summarizer smr = combined_cf.summarize(sum_columns.stream().map( x -> x.name()).collect(Collectors.toList()), AggregateFunctions.sum);

        Table agg_cf = smr.by("dates");

        for( Column<?> cn : sum_columns){
            agg_cf.column("Sum ["+cn.name()+"]").setName(cn.name());
        }
        Cashflow cf_r = new Cashflow(agg_cf);
        return cf_r ;
    }



}
