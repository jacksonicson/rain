package radlab.rain.scoreboard;

import org.junit.Test;

public class TestPoissonSampling {

	@Test
	public void testAccept() {
		PoissonSamplingStrategy strategy = new PoissonSamplingStrategy(-1, "none");
		
		int counter = 0; 
		for(int i=0; i<1000; i++)
		{
			if(strategy.accept(i))
				counter++;
		}
		
		System.out.println("accepted: " + counter); 
	}

}
