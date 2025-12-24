package core;

public class DisjointSet {

    private int[] parent;
    private int connenctedComponentNum;

    public DisjointSet(int size) {
        parent = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = -1;
        }
        connenctedComponentNum = size;
    }

    private int root(int n) {
        if (parent[n] < 0) {
            return n;
        } else {
            int root = root(parent[n]);
            parent[n] = root;
            return root;
        }
    }

    public int componentSize(int n) {
        return -parent[root(n)];
    }

    public int getConnenctedComponentNum() {
        return connenctedComponentNum;
    }

    public boolean isConnected(int n, int m) {
        return root(n) == root(m);
    }

    public void connect(int n, int m) {
        int rootN = root(n),
            rootM = root(m);
        if (rootN == rootM) {
            return;
        }
        if (parent[rootN] < parent[rootM]) {
            parent[rootN] += parent[rootM];
            parent[rootM] = rootN;
        } else {
            parent[rootM] += parent[rootN];
            parent[rootN] = rootM;
        }
        connenctedComponentNum--;
    }
}
