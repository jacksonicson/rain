package radlab.rain.hotspots;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.workload.bookingHotspots.Hotel;

public class Test {

	private static Logger logger = LoggerFactory.getLogger(Test.class);
	
	public static void main(String[] args) {
		logger.info("Uniform multinomial distribution");
		Multinomial m = Multinomial.uniform(100);
		logger.info(m.toString());
		
		logger.info("100 samples with replacement:  ");
		for (Integer i: m.sampleWithReplacement(100))
			System.out.print(i+" ");
		
		logger.info("5 samples without replacement:  ");
		for (Integer i: m.sampleWithoutReplacement(5))
			System.out.print(i+" ");
		
		logger.info("Multinomial distribution following Zipf's law:");
		Multinomial z = Multinomial.zipf(100, 1.5);
		logger.info(z.toString());
		logger.info("distribution with same probabilities, but sorted (notice that the probabilities sharply fall off):");
		logger.info(z.sort(false)+"\n");
		
		logger.info("Multinomial with most of the probabilities set to 0:");
		Multinomial s = Multinomial.sparse(4,20);
		logger.info(s.toString());
		
		
		// create hotel generator and sample from it
		ArrayList<Hotel> hotels = new ArrayList<Hotel>();
		hotels.add( new Hotel("Motel 8",true) );
		hotels.add( new Hotel("Best Western",true) );
		hotels.add( new Hotel("Hilton",true) );
		Multinomial hotelM = Multinomial.zipf(hotels.size(), 1.5);
		IObjectGenerator<Hotel> hotelGenerator = new SimpleObjectGenerator<Hotel>(hotels, hotelM);
		logger.info("\nHotel generator:");
		logger.info("Hotels: "+hotels);
		logger.info("Hotel distribution: "+hotelM);
		System.out.print("Samples: ");
		for (int i=0; i<100; i++)
			System.out.print(hotelGenerator.next()+", ");
	}

	
	public class User {
		public String firstName;
		public String lastName;
		public User(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}
	}
}
