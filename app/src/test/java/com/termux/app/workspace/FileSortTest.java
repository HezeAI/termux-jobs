package com.termux.app.workspace;

import org.junit.Test;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

public class FileSortTest {
    private ProjectFiles.Entry entry(String name,boolean folder,long bytes,long modified){return new ProjectFiles.Entry("/fixture",name,folder,bytes,modified);}
    private List<String> names(List<ProjectFiles.Entry> entries,FileSort.Order order){return FileSort.ordered(entries,order).stream().map(e->e.name).collect(Collectors.toList());}
    @Test public void eachOrderKeepsFoldersFirstAndHasStableNameTies(){
        List<ProjectFiles.Entry> input=Arrays.asList(entry("old",false,4,10),entry("Zdir",true,100,40),entry("new",false,9,30),entry("Adir",true,1,20),entry("same",false,9,30));
        assertEquals(Arrays.asList("Adir","Zdir","new","old","same"),names(input,FileSort.Order.NAME_ASC));
        assertEquals(Arrays.asList("Zdir","Adir","same","old","new"),names(input,FileSort.Order.NAME_DESC));
        assertEquals(Arrays.asList("Adir","Zdir","new","same","old"),names(input,FileSort.Order.SIZE_DESC));
        assertEquals(Arrays.asList("Adir","Zdir","old","new","same"),names(input,FileSort.Order.SIZE_ASC));
        assertEquals(Arrays.asList("Zdir","Adir","new","same","old"),names(input,FileSort.Order.DATE_DESC));
        assertEquals(Arrays.asList("Adir","Zdir","old","new","same"),names(input,FileSort.Order.DATE_ASC));
        assertEquals("old",input.get(0).name); // Display sorting never mutates source/selection ordering.
    }
    @Test public void unknownDatesAreLastAndLongSizesDoNotOverflow(){
        List<ProjectFiles.Entry> input=Arrays.asList(entry("unknown",false,0,-1),entry("large",false,Long.MAX_VALUE,5),entry("small",false,1,10));
        assertEquals(Arrays.asList("large","small","unknown"),names(input,FileSort.Order.DATE_ASC));
        assertEquals(Arrays.asList("small","large","unknown"),names(input,FileSort.Order.DATE_DESC));
        assertEquals(Arrays.asList("large","small","unknown"),names(input,FileSort.Order.SIZE_DESC));
    }
}
