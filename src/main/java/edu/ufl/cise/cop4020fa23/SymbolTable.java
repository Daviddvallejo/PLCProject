package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;

import java.util.*;

import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
import org.apache.commons.lang3.tuple.Pair;

public class SymbolTable {
    private HashMap<Integer, ArrayList<Pair<Integer, NameDef>>> table = new HashMap<>();
    private  Stack<Integer> scope_stack = new Stack<Integer>();
     int current_num;
     int next_num;

    public SymbolTable(){
        this.current_num = 0;
        this.next_num = 1;
    }
    public void enterScope()
    {
        current_num = next_num++;
        scope_stack.push(current_num);
    }
    public void closeScope()
    {
        current_num = scope_stack.pop();
        Iterator<Map.Entry<Integer, ArrayList<Pair<Integer, NameDef>>>> iterator = table.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ArrayList<Pair<Integer, NameDef>>> entry = iterator.next();
            ArrayList<Pair<Integer, NameDef>> pairList = entry.getValue();
            for (Iterator<Pair<Integer, NameDef>> listIterator = pairList.iterator(); listIterator.hasNext(); ) {
                Pair<Integer, NameDef> pair = listIterator.next();
                if (pair.getLeft().equals(current_num)) {
                    listIterator.remove();
                }
            }

        }
        current_num--;
        next_num--;


    }

    public void insert(String key, Pair<Integer, NameDef> attribute) throws TypeCheckException {
        if(table.containsKey(key.hashCode())) {
            ArrayList<Pair<Integer, NameDef>> pairList = table.get(key.hashCode());
            for (Pair<Integer, NameDef> i : pairList) {
                if (i.getLeft() == current_num) {
                    throw new TypeCheckException("Name already in SymbolTable");
                }
            }
            pairList.add(attribute);
        }
        else{
            table.put(key.hashCode(), new ArrayList<>());
            ArrayList<Pair<Integer, NameDef>> pairList = table.get(key.hashCode());
            pairList.add(attribute);
        }
    }
    NameDef lookup(String name) throws TypeCheckException {
        if(table.containsKey(name.hashCode())){
            ArrayList<Pair<Integer, NameDef>> pairList = table.get(name.hashCode());
            pairList.sort(Comparator.comparing(Pair::getLeft, Comparator.reverseOrder()));
            for (Pair<Integer, NameDef> i : pairList) {
                if (i.getLeft() <= current_num) {
                    return i.getRight();
                }
            }
        }
        System.out.println("Name not in SymbolTable");
        return null;
    }
}
