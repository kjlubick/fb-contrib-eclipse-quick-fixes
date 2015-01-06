
public class FloatingCompareToBugs {

    public static class FloatHolder implements Comparable<FloatHolder> {
        float d;
        
        public FloatHolder(float d) {
            this.d = d;
        }

        @Override
        public int compareTo(FloatHolder o) {
            float d1 = d;
            float d2 = o.d;
            return Float.compare(d1, d2);
        }
    }

    public static class FloatHolder2 implements Comparable<FloatHolder2> {
        float d;
        
        public FloatHolder2(float d) {
            this.d = d;
        }

        @Override
        public int compareTo(FloatHolder2 o) {
            return Float.compare(d, o.d);
        }
    }

    public static class DoubleHolder implements Comparable<DoubleHolder> {
        double d;
        
        public DoubleHolder(double d) {
            this.d = d;
        }

        @Override
        public int compareTo(DoubleHolder o) {
            double d1 = d;
            double d2 = o.d;
            return Double.compare(d1, d2);
        }
    }
    
}
