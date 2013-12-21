package org.ancelin.play2.java.couchbase;

public class Row<T> {
    public final T document;
    public final String id;
    public final String key;
    public final String value;
    public Row(T document, String id, String key, String value) {
        this.document = document;
        this.id = id;
        this.key = key;
        this.value = value;
    }
    public Row(String id, String key, String value) {
        this.document = null;
        this.id = id;
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Row{" +
                "document=" + document +
                ", id='" + id + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public boolean isDocumentSet() {
        return this.document == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Row)) return false;

        Row row = (Row) o;

        if (document != null ? !document.equals(row.document) : row.document != null) return false;
        if (id != null ? !id.equals(row.id) : row.id != null) return false;
        if (key != null ? !key.equals(row.key) : row.key != null) return false;
        if (value != null ? !value.equals(row.value) : row.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = document != null ? document.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
