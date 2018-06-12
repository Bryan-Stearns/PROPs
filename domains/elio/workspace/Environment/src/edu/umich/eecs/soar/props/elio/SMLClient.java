package edu.umich.eecs.soar.props.elio;

import sml.Agent;
import sml.Agent.PrintEventInterface;
import sml.Kernel;

public class SMLClient {
	
	public static final PrintEventInterface myPrinter =	new	PrintEventInterface() {
		public void	printEventHandler(int eventID, Object data,	Agent agent, String	message) {
			System.out.println("Soar said: <" + message	+ ">");
		}
	};
	
	public static void main(String[] args) {
		
		Kernel kernel = Kernel.CreateKernelInNewThread();
		kernel.SetAutoCommit(false);

		ElioWorld world = new ElioWorld(kernel);
		
		world.doLevelThresholdSweep("test_elio_props", 2);
		
		//world.runDebug();
			
		
		kernel.Shutdown();
	}

}
