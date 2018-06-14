package edu.umich.eecs.soar.props.cheinmorrison;

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

		EditorsWorld world = new EditorsWorld(kernel);
		world.doLevelThresholdSweep("editors_props",12);

		//world.runDebug();
		
		kernel.Shutdown();
	}

}
