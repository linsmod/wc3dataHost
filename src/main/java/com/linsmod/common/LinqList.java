package com.linsmod.common;

import javax.management.openmbean.InvalidOpenTypeException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

public class LinqList<T> extends ArrayList<T> {

    public LinqList(T... files) {
        List<T> list = Arrays.asList(files);
        this.addAll(list);
    }

    public LinqList(Collection<? extends T> files) {
        files.forEach(x ->
                this.add(x));
    }

    public static LinqList<Integer> of(int[] d) {
        LinqList<Integer> items = new LinqList<>();
        for (int item : d) {
            items.add(item);
        }
        return items;
    }

    public static LinqList<Character> of(char[] d) {
        LinqList<Character> items = new LinqList<>();
        for (char item : d) {
            items.add(Character.valueOf(item));
        }
        return items;
    }

    public LinqList<T> ofType(Class<?> type) {
        return this.ofType(type, null);
    }

    public <E> LinqList<T> ofTypeCall(Class<? extends E> type, Function<E, Boolean> test) {
        return this.where(x -> type.isAssignableFrom(x.getClass()) && callDefault(type, x, test));
    }

    private <E> boolean callDefault(Class<?> type, T o, Function<E, Boolean> function) {
        for (Method method : type.getMethods()) {
            if (method.isDefault()) {
                try {
                    //char d = HasStartToken.getFirstChar()
                    E d = (E) method.invoke(o);

                    // Char.is(char)
                    return function.apply(d);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new RuntimeException("no default method found.");
    }

    public LinqList<T> ofType(Class<?> type, LinqList<T> notList) {
        LinqList<T> items = new LinqList<>();
        for (T v :
                this) {
            if (type.isAssignableFrom(v.getClass())) {
                items.add(v);
            } else if (notList != null) {
                notList.add(v);
            }
        }
        return items;
    }

    public boolean any() {
        return this.size() > 0;
    }

    public <E extends Exception> boolean any(FunctionE<T, E> predicate) throws E {
        for (T t : this) {
            try {
                if (predicate.apply(t))
                    return true;
            } catch (Exception e) {
                throw (E) e;
            }
        }
        return false;
    }

//    public boolean all(Function<T, Boolean> predicate) {
//        if (!this.any())
//            return false;
//        return this.where(predicate).size() == this.size();
//    }

    public <E extends Exception> boolean all(FunctionTRE<T, Boolean, E> predicate) throws E {
        if (!this.any())
            return false;
        for (T t : this) {
            if (!predicate.apply(t)) {
                return false;
            }
        }
        return true;
    }

    public boolean onlyOne() {
        return this.size() == 1;
    }

    public boolean hasOneAndIs(Function<T, Boolean> predicate) {
        return this.size() == 1 && predicate.apply(first());
    }

    public boolean hasOneAndEq(T obj) {
        return this.size() == 1 && linqEquals(obj, first());
    }

    protected boolean linqEquals(T obj, T obj2) {
        return Objects.equals(obj, obj2);
    }

    public boolean overOne() {
        return this.size() > 1;
    }

    public boolean overOne(Function<T, Boolean> predicate) {
        return where(predicate).size() > 2;
    }

    public boolean overTwo() {
        return this.size() > 2;
    }

    public boolean overTwo(Function<T, Boolean> predicate) {
        return where(predicate).size() > 2;
    }

    public int count(Function<T, Boolean> predicate) {
        return where(predicate).size();
    }

    public int countDistinct(Function<T, Object> distinctFn) {
        return distinct(distinctFn).size();
    }

    public T first() {
        if (!any())
            throw new InvalidOpenTypeException("LinqList::first: collection is empty.");
        return this.get(0);
    }

    public T last() {
        return this.get(size() - 1);
    }

    public T first(T defaultValue) {
        if (!this.any()) {
            return defaultValue;
        }
        return this.get(0);
    }

    public T firstOrNull() {
        return first((T) null);
    }

    public T first(Function<T, Boolean> d) {
        return this.where(d, null).first();
    }

    public LinqList<T> where(Function<T, Boolean> d) {
        return this.where(d, null);
    }

    public LinqList<T> where(Function2<T, Integer, Boolean> d) {
        return this.where(d, null);
    }

    public LinqList<T> whereNotNull() {
        return where(Objects::nonNull);
    }

    public LinqList<T> where(Function<T, Boolean> d, LinqList<T> notList) {
        LinqList<T> items = new LinqList<>();
        this.forEach(x -> {
            if (d.apply(x)) {
                items.add(x);
            } else if (notList != null) {
                notList.add(x);
            }
        });
        return items;
    }

    public LinqList<T> where(Function2<T, Integer, Boolean> d, LinqList<T> notList) {
        LinqList<T> items = new LinqList<>();
        for (int i = 0; i < this.size(); i++) {
            T x = this.get(i);
            if (d.apply(x, i)) {
                items.add(x);
            } else if (notList != null) {
                notList.add(x);
            }
        }
        return items;
    }

    public LinqList<T> except(Function<T, Boolean> d) {
        LinqList<T> items = new LinqList<>();
        this.forEach(x -> {
            if (!d.apply(x)) {
                items.add(x);
            }
        });
        return items;
    }

    public LinqList<T> except(List<T> targets) {
        LinqList<T> items = new LinqList<>();
        this.forEach(x -> {
            if (!targets.contains(x)) {
                items.add(x);
            }
        });
        return items;
    }

    public LinqList<T> except(List<T> targets, Function<T, Object> equalBy) {
        LinqList<T> targetList = new LinqList<>(targets);
        LinqList<T> items = new LinqList<>();
        this.forEach(x -> {
            if (!targetList.any(y -> equalBy.apply(x).equals(equalBy.apply(y)))) {
                items.add(x);
            }
        });
        return items;
    }

    public boolean hasTwo() {
        return size() == 2;
    }

    public boolean hasTwo(Function<T, Boolean> predicate) {
        return where(predicate).size() == 2;
    }

    public LinqList<T> skip(int n) {
        LinqList list = new LinqList();
        for (int i = n; i < size(); i++) {
            list.add(get(i));
        }
        return list;
    }

    public <TResult> LinqList<TResult> autoType() {
        return this.select(x -> (TResult) x);
    }

//    public <TResult> TResult[] autoArray() {
//        TResult[] array = (TResult[]) new Object[size()];
//        for (int i = 0; i < size(); i++) {
//            Array.set(array, i, get(i));
//        }
//        return array;
//    }

    public <TResult, E extends Exception> LinqList<TResult> select(FunctionTRE<T, TResult, E> selector) throws E {
        LinqList<TResult> items = new LinqList<>();
        for (T t : this) {
            try {
                items.add(selector.apply(t));
            } catch (Exception e) {
                throw (E) e;
            }
        }
        return items;
    }

    public <TResult> LinqList<TResult> select(Function2<T, Integer, TResult> selector) {
        LinqList<TResult> items = new LinqList<>();
        for (int i = 0; i < size(); i++) {
            items.add(selector.apply(this.get(i), i));
        }
        return items;
    }

    public void foreach(Action2<T, Integer> selector) {
        for (int i = 0; i < size(); i++) {
            selector.apply(this.get(i), i);
        }
    }

    public void foreach(Action4<T, Integer> selector) {
        for (int i = 0; i < size(); i++) {
            selector.apply(this.get(i), i, i == 0, i == size() - 1);
        }
    }

    public <E extends Exception> void foreach(ActionE<T, E> selector) throws E {
        for (int i = 0; i < size(); i++) {
            try {
                selector.apply(this.get(i));
            } catch (Exception e) {
                throw (E) e;
            }
        }
    }

    public <TResult> LinqList<TResult> select(Function3<LinqList<T>, T, Integer, TResult> selector) {
        LinqList<TResult> items = new LinqList<>();
        for (int i = 0; i < size(); i++) {
            items.add(selector.apply(this, this.get(i), i));
        }
        return items;
    }


    public String join(String s) {
        return String.join(s, this.select(x -> x.toString()));
    }

    public String join(Function2<T, T, String> idj) {
        if (onlyOne()) {
            return idj.apply(null, first());
        }
        String d = "";
        T last = null;
        for (T t : this) {
            if (last == null) {
                last = t;
            } else {
                d += idj.apply(last, t);
                last = t;
            }
        }
        return d;
    }

    public LinqList<T> shuffle() {
        LinqList<T> ts = new LinqList<>(this);
        Collections.shuffle(ts);
        return ts;
    }

    public T find(Function<T, Boolean> predicate) {
        for (T t : this) {
            if (predicate.apply(t)) {
                return t;
            }
        }
        return null;
    }

    public autoList<T> autoList() {
        return new autoList<>(this);
    }

    public int countTail(Object target) {
        int n = 0;
        for (int i = size() - 1; i >= 0; i--) {
            if (get(i) != target) {
                break;
            }
            n++;
        }
        return n;
    }

    public int countTail(Object target, Function<T, Object> selector) {
        int n = 0;
        Object apply = selector.apply((T) target);
        for (int i = size() - 1; i >= 0; i--) {
            Object apply1 = selector.apply(get(i));
            if (!apply1.equals(apply)) {
                break;
            }
            n++;
        }
        return n;
    }

    public int countTailRepeats(Function<T, Object> selector) {
        if (size() == 0)
            return 0;
        return countTail(this.get(size() - 1), selector);
    }

    public boolean allEquals(Function<T, Object> selector) {
        return countTail(first(), selector) == size();
    }

    public boolean sequenceEqual(List<T> elements) {
        if (size() != elements.size()) {
            return false;
        }
        for (int i = 0; i < size(); i++) {
            Object o1 = get(i);
            Object apply = elements.get(i);
            if (!o1.equals(apply)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从头向尾直到True，
     * 然后从尾向前直到True。
     * 不是一直从头到尾。
     *
     * @param predicate
     * @return
     */
    public LinqList<T> takeEnclosed(Function<T, Boolean> predicate) {
        LinqList<T> items = new LinqList<>(this);
        while (items.any() && !predicate.apply(items.first())) {
            items = items.skip(1);
        }
        while (items.any() && !predicate.apply(items.last())) {
            items.remove(items.size() - 1);
        }
        return items;
    }

    public LinqList<T> takeEnclosed(Function<T, Boolean> open, Function<T, Boolean> close) {
        LinqList<T> items = new LinqList<>(this);
        while (items.any() && !open.apply(items.first())) {
            items = items.skip(1);
        }
        while (items.any() && !close.apply(items.last())) {
            items.remove(items.size() - 1);
        }
        return items;
    }

    public boolean sequenceEqual(List<T> elements, Function<T, Object> o) {
        if (size() != elements.size()) {
            return false;
        }
        for (int i = 0; i < size(); i++) {
            Object o1 = get(i, o);
            Object apply = o.apply(elements.get(i));
            if (!o1.equals(apply)) {
                return false;
            }
        }
        return true;
    }

    private Object get(int i, Function<T, Object> o) {
        return o.apply(get(i));
    }

    private boolean equals(T a, T b, Function<T, Object> o) {
        return o.apply(a).equals(o.apply(b));
    }

    public void addAll(T[] values, boolean distinct) {
        if (distinct) {
            new LinqList<>(values).forEach(x -> {
                if (!this.contains(x))
                    this.add(x);
            });
        } else {
            this.addAll(values);
        }
    }

    public void addAll(T[] values) {
        this.addAll(new LinqList<>(values));
    }

    public void addAll(LinqList<T> values, boolean distinct) {
        if (distinct) {
            values.forEach(x -> {
                if (!this.contains(x))
                    this.add(x);
            });
        } else {
            this.addAll(values);
        }
    }

    public LinqList<T> addAll(Collection<? extends T> values, Function<T, Object> distinctBy) {
        LinqList<T> list = new LinqList<>();
        for (T value : values) {
            Object input = distinctBy.apply(value);
            if (!this.any(x -> distinctBy.apply(x).equals(input))) {
                this.add(value);
                list.add(value);
            }
        }
        return list;
    }

    public LinqList<T> distinct() {
        if (!any())
            return new LinqList<>();
        LinqList<T> items = new LinqList<>();
        for (T item : this) {
            if (!items.any(x -> x.equals(item))) {
                items.add(item);
            }
        }
        return items;
    }

    public LinqList<T> distinctSelf(Function<T, Object> by) {
        LinqList<T> distinct = this.distinct(by);
        this.clear();
        this.addAll(distinct);
        return this;
    }

    public LinqList<T> distinct(Function<T, Object> by) {
        if (!any())
            return new LinqList<>();
        LinqList<T> items = new LinqList<>();
        for (T item : this) {
            if (!items.any(x -> by.apply(x).equals(by.apply(item)))) {
                items.add(item);
            }
        }
        return items;
    }

    public LinqList<T> toLinqList() {
        return new LinqList<>(this);
    }

    public LinqList<T> concat(LinqList<T> items) {
        LinqList<T> ts = new LinqList<>(this);
        ts.addAll(items);
        return ts;
    }

    public boolean contains(T target, Function<T, Object> by) {
        return where(x -> by.apply(x).equals(by.apply(target))).any();
    }

    public <E> LinqList<E> selectMany(Function<T, List<E>> itemsFn) {
        LinqList<E> items = new LinqList<>();
        for (T t : this) {
            items.addAll(itemsFn.apply(t));
        }
        return items;
    }

    public void addDistinct(Iterable<? extends T> list, Function<T, Object> by) {
        for (T t : list) {
            if (!this.contains(t, by)) {
                super.add(t);
            }
        }
    }

    public Object[] objectArray() {
        return this.toArray(Object[]::new);
    }

    public int[] toIntArray() {
        int[] array = new int[size()];
        this.foreach((x, i) -> array[i] = (int) x);
        return array;
    }

    /**
     * to distinctList, and you got a distinct-ed list.
     *
     * @param equallyComparer
     * @return
     */
    public DistinctList<T> toDistinctList(Function2<T, T, Boolean> equallyComparer) {
        DistinctList<T> list = new DistinctList<>(equallyComparer);
        for (T t : this) {
            list.add(t);
        }
        return list;
    }

    public double sumDouble(Function<T, Object> selector) {
        double n = 0;
        for (T t : this) {
            double apply = (double) selector.apply(t);
            n = Double.sum(apply, n);
        }
        return n;
    }

    public double sum(Function<T, Number> selector) {
        double n = 0;
        for (T t : this) {
            double apply = num2double(selector.apply(t));
//            System.out.println("double:" + apply + " of " + t);
            n = Double.sum(apply, n);
        }
        return n;
    }

    private double num2double(Number num) {
        return num.doubleValue();
    }

    public <TResult> TResult max() {
        return max(x -> x);
    }

    public <TResult> TResult max(Function<T, Object> selector) {
        double n = 0;
        for (T t : this) {
            Object d = selector.apply(t);
            if (d != null && d.getClass().equals(Double.class)) {
                double apply = (double) d;
                n = Double.max(apply, n);
            } else if (d != null && d.getClass().equals(Integer.class)) {
                double apply = (double) d;
                n = Double.max(apply, n);
            } else {
                throw new RuntimeException("max: only double and int is supported.");
            }
        }
        return (TResult) (Double) n;
    }

    public int indexOf(Function<T, Boolean> predicate) {
        for (int i = 0; i < size(); i++) {
            if (predicate.apply(this.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public T singleOrDefault() {
        if (size() == 0)
            return null;
        return single();
    }

    public T single() {
        if (overOne()) {
            throw new RuntimeException("LinqList.single meets overOne!");
        }
        return get(0);
    }

    public LinqList<SplitGroup> split(Function<T, Boolean> splitBy) {
        LinqList<SplitGroup> groups = new LinqList<>();
        SplitGroup group = null;
        for (T t : this) {
            if (splitBy.apply(t)) {
                if (group == null) {
                    group = new SplitGroup(SplitOption.Default);
                    group.seeBeforeUsingOption = true;
                }
                group.seeBehindUsingOption = true;
                group.splitBy = t;
                groups.add(group);

                group = new SplitGroup(SplitOption.Default);
                group.seeBeforeUsingOption = true;
                group.splitBy = t;
            } else {
                if (group == null) {
                    group = new SplitGroup(SplitOption.Default);
                }
                group.add(t);
            }
        }
        if (group != null) {
            groups.add(group);
            group = null;
        }
        return groups.where(x -> !x.isEmpty());
    }

    public LinqList<SplitGroup> split(Function<T, Boolean> splitBy, SplitOption opt) {

        LinqList<SplitGroup> groups = new LinqList<>();
        SplitGroup group = null;
        for (T t : this) {
            if (splitBy.apply(t)) {
                if (group == null) {
                    group = new SplitGroup(opt);
                    group.seeBeforeUsingOption = opt == SplitOption.Default || opt == SplitOption.SplitterAsLeading;
                }
                //prev
                group.splitBy = t;
                group.seeBehindUsingOption = opt == SplitOption.Default || opt == SplitOption.SplitterAsTailing;
                groups.add(group);

                //next
                group = new SplitGroup(opt);
                group.splitBy = t;
                group.seeBeforeUsingOption = opt == SplitOption.Default || opt == SplitOption.SplitterAsLeading;
                ;
            } else {
                if (group == null) {
                    group = new SplitGroup(opt);
                }
                group.add(t);
            }
        }
        if (group != null) {
            groups.add(group);
            group = null;
        }
        return groups.where(x -> !x.isEmpty());
    }

    public boolean firstIs(Function<T, Boolean> check) {
        return any() && check.apply(first());
    }

    public boolean lastIs(Function<T, Boolean> check) {
        return any() && check.apply(last());
    }

    public LinqList<T> orderBy(Function<T, Object> selector) {
        Object[] array = this.toArray();
        Arrays.sort(array, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                Object left = selector.apply((T) o1);
                Object right = selector.apply((T) o2);
                if (left instanceof Integer || left.getClass().getName().equals("int")) {
                    return Integer.compare((Integer) left, (Integer) right);
                }
                if (left instanceof Long || left.getClass().getName().equals("long")) {
                    return Long.compare((Integer) left, (Integer) right);
                }
                if (left instanceof Double || left.getClass().getName().equals("double")) {
                    return Long.compare((Integer) left, (Integer) right);
                }
                throw new RuntimeException("LinqList.orderBy() only allows comparing int double or long");
            }
        });
        LinqList<T> output = new LinqList<>();
        for (Object o : array) {
            output.add((T) o);
        }
        return output;
    }

    public enum SplitOption {
        /**
         * 标准模式，分割符共享。
         * 如果是 #123#456#789，按#前导分割，那么结果为（1）#123#，（2）#456#，（3）#789，第一组和第二组既有前见也有后见。
         * 如果是 123#456#789，按#前导分割，那么结果为（1）123#，（2）#456#，（3）#789
         * 只有中间组既有前见也有后见。
         */
        Default,

        /**
         * 前导模式，分割符作引导标记。
         * 如果是 #123#456#789，按#前导分割, 那么 (1)#123，(2)#456,(3)#789，每组都有前见，无后见。
         * 如果是 123#456#789#，按#前导分割, 那么(1)123,（2）#456,（3）#789，第一组无前见，其他都有前见。
         */
        SplitterAsLeading,

        /**
         * 尾巴模式，分割符作结尾标记。
         * 如果是 #123#456#789按#分割, 那么（1）#，（2）123#，（3）456#，（4）789，第一组是后见尾巴+空组，最后一组是后无。
         * 如果是 123#456#789#, 那么(1)123#,（2）456#,（3）789#，每一组都有后见#。
         */
        SplitterAsTailing
    }

    public class SplitGroup extends LinqList<T> {
        /**
         * can be null if split not applied on sequence that does not contain any splitter element
         */
        public T splitBy;
        /**
         * 在前见模式中出现在前面，或者标准模式中出现。请参考SplitOption说明。
         */
        public boolean seeBeforeUsingOption;
        /**
         * 在后见模式中出现在后面，或者标准模式中出现。请参考SplitOption说明。
         */
        public boolean seeBehindUsingOption;
        SplitOption opt;

        public SplitGroup(SplitOption opt) {
            super();
            this.opt = opt;
        }
    }
}
