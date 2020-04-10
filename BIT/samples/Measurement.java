import java.util.Objects;


public class Measurement {
    int i_count, b_count, m_count;

    public Measurement(int i, int b, int m){
        this.i_count = i;
        this.b_count = b;
        this.m_count = m;
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        Measurement that = (Measurement) object;
        return i_count == that.i_count &&
                b_count == that.b_count &&
                m_count == that.m_count;
    }

    public int hashCode() {
        return Objects.hash(super.hashCode(), i_count, b_count, m_count);
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Measurement{" +
                "i_count=" + i_count +
                ", b_count=" + b_count +
                ", m_count=" + m_count +
                '}';
    }
}