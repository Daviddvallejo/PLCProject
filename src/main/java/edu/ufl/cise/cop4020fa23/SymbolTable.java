package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.Map;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
import org.apache.commons.lang3.tuple.Pair;
public class SymbolTable {
    private HashMap<String, Pair<Integer, NameDef>> table = new HashMap<>();
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
    }

    public void insert(String key, Pair<Integer, NameDef> attribute) throws TypeCheckException {
        if(table.containsKey(key)){
            throw new TypeCheckException("Name already in SymbolTable");
        }
        else{
            table.put(key, attribute);
        }
    }
    NameDef lookup(String name) throws TypeCheckException {
        for (Map.Entry<String, Pair<Integer, NameDef>> entry : table.entrySet()) {
            String key = entry.getKey();
            Pair<Integer, NameDef>  value = entry.getValue();
            if(key.equals(name)){
                for(int i = current_num; i >= 0; i--){
                    if(i == value.getLeft()){
                        return value.getRight();
                    }
                }
            }
        }
        System.out.println("Name not in SymbolTable");
        return null;
    }
}
