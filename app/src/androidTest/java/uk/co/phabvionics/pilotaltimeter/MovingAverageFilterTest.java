package uk.co.phabvionics.pilotaltimeter;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by david on 28/07/2015.
 */
public class MovingAverageFilterTest {

    @Test
    void MovingAverageFilter()
    {
        MovingAverageFilter maf = new MovingAverageFilter(3);
        assertEquals(1, maf.Filter(3), 0);
        assertEquals(2, maf.Filter(3), 0);
        assertEquals(3, maf.Filter(3), 0);
        assertEquals(3, maf.Filter(3), 0);
        assertEquals(2, maf.Filter(0), 0);
        assertEquals(1, maf.Filter(0), 0);
        assertEquals(0, maf.Filter(0), 0);
        assertEquals(0, maf.Filter(0), 0);
    }
}