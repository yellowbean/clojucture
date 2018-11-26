package java;
import tech.tablesaw.api.Table;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.columns.Column;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Stream;
import tech.tablesaw.columns.AbstractColumn;
import java.time.LocalDate;

/*
public class Statement {
    private String name;
    private Table records;

    Statement(){

    }

    Statement(String name){
        this.name = name;
        this.records = Table.create(name);
        DoubleColumn amt = DoubleColumn.create("Amount");
        DateColumn dt = DateColumn.create("Date");
        StringColumn to = StringColumn.create("to");
        StringColumn from = StringColumn.create("from");
        StringColumn info = StringColumn.create("info");
        this.records.addColumns([dt,from,to,amt,info]);
    }

    public static Statement create(String name){
        return new Statement(name);
    }

}
*/