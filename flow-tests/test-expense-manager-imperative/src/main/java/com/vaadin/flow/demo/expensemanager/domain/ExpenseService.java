package com.vaadin.flow.demo.expensemanager.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import elemental.json.Json;
import elemental.json.JsonArray;

public class ExpenseService {

    private static int SIZE = 1500;

    public static class Filters {
        public LocalDate from, to;
        public Double min, max;
        public String merch;
        public boolean neW, prog, reim;

        public int count() {
            int c = 0;
            if (from != null)
                c++;
            if (to != null)
                c++;
            if (min != null)
                c++;
            if (max != null)
                c++;
            if (merch != null)
                c++;
            if (neW)
                c++;
            if (prog)
                c++;
            if (reim)
                c++;
            return c;
        }
    }

    public static String[] merchants = { "Parking", "Ride sharing",
            "Restaurant", "Shuttle", "Rental car", "Fast food", "Taxi",
            "Electronics", "Breakfast", "Hotel", "Airline", "Office supplies" };
    static String[] statuses = { "new", "in-progress", "reimbursed" };
    static String[] comments = { "Expense from my business trip" };

    public static final ExpenseService INSTANCE = new ExpenseService();

    private final ConcurrentMap<Integer, Expense> expenses = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(-1);

    private ExpenseService() {
        Random random = new Random(0);
        LocalDate time = LocalDate.now();
        int f = Math.max(2, 2000 / SIZE);
        System.out.println(SIZE + " " + f);
        for (int i = 0, l = SIZE; i < l; i++) {
            Expense expense = new Expense();
            expense.setComment(comments[random.nextInt(comments.length)]);
            expense.setStatus(statuses[random.nextInt(statuses.length)]);
            expense.setMerchant(merchants[random.nextInt(merchants.length)]);
            time = time.plusDays(-random.nextInt(f));
            if (time.isBefore(LocalDate.now().minusMonths(6))) {
                expense.setStatus(statuses[2]);
            }
            if (time.isAfter(LocalDate.now().minusDays(10))) {
                expense.setStatus(statuses[0]);
            }
            expense.setTotal(random.nextDouble() * 200);
            expense.setDate(
                    time.atStartOfDay(ZoneId.systemDefault()).toLocalDate());
            save(expense);
        }
    }

    public static ExpenseService getDemoService() {
        return INSTANCE;
    }

    public Stream<Expense> expenseStream() {
        return expenses.values().stream().map(Expense::copy);
    }

    public List<Expense> findAll(Filters f) {
        return filterStream(expenseStream(), f).collect(Collectors.toList());
    }

    public double getTotal(Filters f) {
        // @formatter:off
        return filterStream(expenseStream(), f)
                .map(entry -> entry.getTotal())
                .reduce((acc, expenseTotal) -> acc += expenseTotal)
                .orElse(0.0);
        // @formatter:on
    }

    public JsonArray toJson(Filters f, int col, int asc, int index, int count) {
        return toJson(sliceStream(
                filterStream(sortStream(expenseStream(), col, asc), f), index,
                count).collect(Collectors.toList()));
    }

    private JsonArray toJson(List<Expense> exps) {
        JsonArray v = Json.instance().createArray();
        for (Expense e : exps) {
            v.set(v.length(), e.toJson());
        }
        return v;
    }

    public synchronized int count() {
        return expenses.size();
    }

    public void delete(Expense value) {
        expenses.remove(value.getId());
    }

    public void save(Expense entry) {
        if (entry.getId() == null) {
            entry.setId(nextId.incrementAndGet());
        }
        expenses.put(entry.getId(), entry);
    }

    public String getExpenseIdByPosition(int pos, Filters f, int col, int asc) {
        return "" + filterStream(sortStream(expenseStream(), col, asc), f)
                .collect(Collectors.toList()).get(pos).getId();
    }

    public Optional<Expense> getExpense(String expenseId) {
        int id = -1;
        try {
            id = Integer.valueOf(expenseId);
        } catch (NumberFormatException ignore) {
        }
        return Optional.ofNullable(expenses.get(id));
    }

    public Stream<Expense> sortStream(Stream<Expense> stream, int col,
            int asc) {
        return stream.sorted((f1, f2) -> {
            if (col == 4)
                return asc * f1.getComment().compareTo(f2.getComment());
            if (col == 3)
                return asc * f1.getStatus().compareTo(f2.getStatus());
            if (col == 2)
                return asc * f1.getTotal().compareTo(f2.getTotal());
            if (col == 1)
                return asc * f1.getMerchant().compareTo(f2.getMerchant());
            return asc * f1.getDate().compareTo(f2.getDate());
        });
    }

    public Stream<Expense> filterStream(Stream<Expense> stream, Filters f) {
        return stream.filter(o -> {
            boolean r = false;
            if (f.neW)
                r = r || statuses[0].equals(o.getStatus());
            if (f.prog)
                r = r || statuses[1].equals(o.getStatus());
            if (f.reim)
                r = r || statuses[2].equals(o.getStatus());
            r = r || !f.neW && !f.prog && !f.reim;
            if (f.from != null)
                r = r && o.getDate().isAfter(f.from);
            if (f.to != null)
                r = r && o.getDate().isBefore(f.to);
            if (f.min != null)
                r = r && o.getTotal() >= f.min;
            if (f.max != null)
                r = r && o.getTotal() <= f.max;
            if (f.merch != null)
                r = r && f.merch.equals(o.getMerchant());
            return r;
        });
    }

    public Stream<Expense> sliceStream(Stream<Expense> stream, int i, int c) {
        return stream.skip(i).limit(c);
    }

    public JsonArray computeChartData(StringBuilder categories,
            Filters filters) {
        JsonArray dataArray = Json.createArray();
        List<Expense> expenses = findAll(filters);
        double value = 0;
        Expense expense = null;
        for (int i = 0; i < expenses.size(); i++) {
            if (expense != null
                    && expense.getDate().getMonthValue() != expenses.get(i)
                            .getDate().getMonthValue()) {
                if (value > 0) {
                    dataArray.set(dataArray.length(), value);

                    String monthName = expense.getDate().getMonth()
                            .getDisplayName(TextStyle.SHORT, Locale.US);
                    String year = "";
                    if (dataArray.length() == 1
                            || expense.getDate().getYear() != expenses.get(i)
                                    .getDate().getYear()) {
                        year = expense.getDate().getYear() + " ";
                    }

                    categories.append(year + monthName + ",");
                    value = 0;
                }
            }

            expense = expenses.get(i);
            value += expense.getTotal();
            if (expense.getDate().isBefore(LocalDate.now().minusYears(1))) {
                break;
            }
        }
        return dataArray;
    }

    public int getSize(Filters f) {
        return (int) filterStream(expenseStream(), f).count();
    }
}
