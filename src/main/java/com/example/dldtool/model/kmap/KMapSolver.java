package com.example.dldtool.model.kmap;

import java.util.*;

public class KMapSolver {

    private int numVars;
    private int[] cells; // 0=zero, 1=one, 2=don't care
    private List<String> primeImplicants = new ArrayList<>();
    private List<Set<Integer>> groups = new ArrayList<>();

    // Gray code orders for K-map rows/cols
    public static final int[] GRAY_2 = {0, 1};
    public static final int[] GRAY_4 = {0, 1, 3, 2};
    public static final int[] GRAY_8 = {0, 1, 3, 2, 6, 7, 5, 4};

    public KMapSolver(int numVars) {
        this.numVars = numVars;
        this.cells = new int[(int) Math.pow(2, numVars)];
    }

    public void setCell(int index, int value) {
        if (index >= 0 && index < cells.length) cells[index] = value;
    }

    public int getCell(int index) { return cells[index]; }
    public int getNumVars()       { return numVars; }
    public int getTotalCells()    { return cells.length; }
    public List<Set<Integer>> getGroups() { return groups; }

    // ── MAIN SOLVE ────────────────────────────────────────────
    public String solve() {
        primeImplicants.clear();
        groups.clear();

        List<Integer> minterms  = new ArrayList<>();
        List<Integer> dontCares = new ArrayList<>();

        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == 1) minterms.add(i);
            if (cells[i] == 2) dontCares.add(i);
        }

        if (minterms.isEmpty()) return "F = 0";

        List<Integer> allOnes = new ArrayList<>(minterms);
        allOnes.addAll(dontCares);
        if (minterms.size() == cells.length ||
                allOnes.size() == cells.length && minterms.size() > 0) {
            // Check if all real cells are 1
            boolean allReal = true;
            for (int i = 0; i < cells.length; i++) {
                if (cells[i] == 0) { allReal = false; break; }
            }
            if (allReal) return "F = 1";
        }

        // Run Quine-McCluskey
        List<Implicant> primes = findPrimeImplicants(minterms, dontCares);
        List<Implicant> essential = findEssentialImplicants(primes, minterms);

        // Build groups for highlighting
        for (Implicant imp : essential) {
            groups.add(new HashSet<>(imp.minterms));
        }

        // Build expression
        if (essential.isEmpty()) return "F = 0";

        List<String> terms = new ArrayList<>();
        for (Implicant imp : essential) {
            terms.add(imp.toExpression(numVars));
        }
        return "F = " + String.join(" + ", terms);
    }

    // ── QUINE-McCLUSKEY ───────────────────────────────────────
    private List<Implicant> findPrimeImplicants(
            List<Integer> minterms, List<Integer> dontCares) {

        List<Integer> all = new ArrayList<>(minterms);
        all.addAll(dontCares);

        // Initial implicants — one per minterm
        List<Implicant> current = new ArrayList<>();
        for (int m : all) {
            Implicant imp = new Implicant(numVars);
            imp.mask = 0;
            imp.value = m;
            imp.minterms.add(m);
            current.add(imp);
        }

        List<Implicant> primes = new ArrayList<>();
        Set<String> primeKeys  = new HashSet<>();

        while (!current.isEmpty()) {
            List<Implicant> next    = new ArrayList<>();
            boolean[] used          = new boolean[current.size()];

            for (int i = 0; i < current.size(); i++) {
                for (int j = i + 1; j < current.size(); j++) {
                    Implicant a = current.get(i);
                    Implicant b = current.get(j);
                    Implicant combined = tryMerge(a, b);
                    if (combined != null) {
                        used[i] = true;
                        used[j] = true;
                        // Avoid duplicates
                        String key = combined.value + "m" + combined.mask;
                        if (!primeKeys.contains(key)) {
                            next.add(combined);
                            primeKeys.add(key);
                        }
                    }
                }
            }

            for (int i = 0; i < current.size(); i++) {
                if (!used[i]) {
                    String key = current.get(i).value
                            + "m" + current.get(i).mask + "p";
                    if (!primeKeys.contains(key)) {
                        primes.add(current.get(i));
                        primeKeys.add(key);
                    }
                }
            }
            current = next;
        }
        return primes;
    }

    private Implicant tryMerge(Implicant a, Implicant b) {
        if (a.mask != b.mask) return null;
        int diff = (a.value ^ b.value) & ~a.mask;
        // Must differ by exactly one bit
        if (diff == 0 || (diff & (diff - 1)) != 0) return null;

        Implicant merged = new Implicant(numVars);
        merged.mask  = a.mask | diff;
        merged.value = a.value & ~diff;
        merged.minterms.addAll(a.minterms);
        merged.minterms.addAll(b.minterms);
        return merged;
    }

    private List<Implicant> findEssentialImplicants(
            List<Implicant> primes, List<Integer> minterms) {

        List<Implicant> essential = new ArrayList<>();
        Set<Integer> covered      = new HashSet<>();

        // Find essential prime implicants
        for (int minterm : minterms) {
            List<Implicant> covering = new ArrayList<>();
            for (Implicant p : primes) {
                if (p.covers(minterm)) covering.add(p);
            }
            if (covering.size() == 1) {
                Implicant e = covering.get(0);
                if (!essential.contains(e)) {
                    essential.add(e);
                    covered.addAll(e.minterms);
                }
            }
        }

        // Cover remaining minterms greedily
        for (int minterm : minterms) {
            if (!covered.contains(minterm)) {
                Implicant best = null;
                int bestCover  = -1;
                for (Implicant p : primes) {
                    if (p.covers(minterm)) {
                        int newCover = 0;
                        for (int m : p.minterms) {
                            if (!covered.contains(m)) newCover++;
                        }
                        if (newCover > bestCover) {
                            bestCover = newCover;
                            best = p;
                        }
                    }
                }
                if (best != null && !essential.contains(best)) {
                    essential.add(best);
                    covered.addAll(best.minterms);
                }
            }
        }
        return essential;
    }

    // ── IMPLICANT INNER CLASS ─────────────────────────────────
    public static class Implicant {
        int numVars;
        int value; // the base value (0s and 1s)
        int mask;  // 1 = don't care bit position
        List<Integer> minterms = new ArrayList<>();

        Implicant(int numVars) { this.numVars = numVars; }

        boolean covers(int minterm) {
            return (minterm & ~mask) == (value & ~mask);
        }

        String toExpression(int numVars) {
            String[] varNames = {"A","B","C","D","E","F"};
            StringBuilder sb = new StringBuilder();
            for (int i = numVars - 1; i >= 0; i--) {
                int bit = 1 << i;
                if ((mask & bit) == 0) {
                    // This bit matters
                    String varName = varNames[numVars - 1 - i];
                    if ((value & bit) == 0) sb.append(varName).append("'");
                    else sb.append(varName);
                }
            }
            if (sb.length() == 0) return "1";
            return sb.toString();
        }
    }

    // ── K-MAP LAYOUT HELPERS ──────────────────────────────────
    // Returns the minterm index for a given row, col in the K-map grid
    public int getMintermIndex(int row, int col) {
        switch (numVars) {
            case 2: return (GRAY_2[row] << 1) | GRAY_2[col];
            case 3: return (GRAY_2[row] << 2) | GRAY_4[col];
            case 4: return (GRAY_4[row] << 2) | GRAY_4[col];
            case 5: return (GRAY_4[row] << 3) | GRAY_8[col] |
                    ((row >= 4 ? 1 : 0) << 4);
            case 6: return (GRAY_4[row] << 4) | GRAY_4[col] |
                    ((GRAY_4[row] >> 2) << 5);
            default: return 0;
        }
    }

    public int getNumRows() {
        switch (numVars) {
            case 2: return 2;
            case 3: return 2;
            case 4: return 4;
            case 5: return 4;
            case 6: return 4;
            default: return 2;
        }
    }

    public int getNumCols() {
        switch (numVars) {
            case 2: return 2;
            case 3: return 4;
            case 4: return 4;
            case 5: return 8;
            case 6: return 8;
            default: return 2;
        }
    }

    public String[] getRowLabels() {
        switch (numVars) {
            case 2: return new String[]{"A=0","A=1"};
            case 3: return new String[]{"A=0","A=1"};
            case 4: return new String[]{"AB=00","AB=01","AB=11","AB=10"};
            case 5: return new String[]{"AB=00","AB=01","AB=11","AB=10"};
            case 6: return new String[]{"AB=00","AB=01","AB=11","AB=10"};
            default: return new String[]{};
        }
    }

    public String[] getColLabels() {
        switch (numVars) {
            case 2: return new String[]{"B=0","B=1"};
            case 3: return new String[]{"BC=00","BC=01","BC=11","BC=10"};
            case 4: return new String[]{"CD=00","CD=01","CD=11","CD=10"};
            case 5: return new String[]{"CDE=000","CDE=001","CDE=011",
                    "CDE=010","CDE=110","CDE=111",
                    "CDE=101","CDE=100"};
            case 6: return new String[]{"CD=00","CD=01","CD=11","CD=10",
                    "CD=00","CD=01","CD=11","CD=10"};
            default: return new String[]{};
        }
    }
}