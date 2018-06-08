/**
 * Class representing a single line of input data.
 *
 * Data format of input file must be MODEL;MODELYEAR;INVOICEDATE;RABATT;MIT_RABATT;OHNE_RABATT
 *  with RABATT be the rebate of the day in percent (not in any currency!!)
 *  with MIT_RABATT be the sales with that rebate on the day
 *  with OHNE_RABATT be the sales without rebate on that day
 * The data has to be ordered by INVOICEDATE, MODEL, MODELYEAR
 *
 * @author Leo Knoll
 */
public class RebateData {

    private int name;
    private int year;
    private long date;
    private double rebate;
    private int salesRebate;
    private int salesWithout;


    public RebateData() {

    }

    public RebateData(int name, int year, long date, double rebate, int salesRebate, int salesWithout) {
        this.name = name;
        this.year = year;
        this.date = date;
        this.rebate = rebate;
        this.salesRebate = salesRebate;
        this.salesWithout = salesWithout;
    }


    // getters and setters

    public int getName() {
        return name;
    }

    public void setName(int name) {
        this.name = name;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public double getRebate() {
        return rebate;
    }

    public void setRebate(double rebate) {
        this.rebate = rebate;
    }

    public int getSalesRebate() {
        return salesRebate;
    }

    public void setSalesRebate(int salesRebate) {
        this.salesRebate = salesRebate;
    }

    public int getSalesWithout() {
        return salesWithout;
    }

    public void setSalesWithout(int salesWithout) {
        this.salesWithout = salesWithout;
    }

    @Override
    public String toString() {
        return "Name: " + name + ", Year: " + year + ", Date: " + date
            + ", Rebate: " + rebate + ", Sales: " + salesRebate + " / " + salesWithout;
    }
}
