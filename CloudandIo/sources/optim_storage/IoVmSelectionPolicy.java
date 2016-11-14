
package optim_storage;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Vm;

import io_storage.IoHost;
import io_storage.IoVm;

/**
 * The class of an abstract VM selection policy with storage supporting. 
 * @author Hamza Ouarnoughi
 * @since CloudSim Toolkit 3.0
 */
public abstract class IoVmSelectionPolicy {

	/**
	 * Gets the vms to migrate.
	 * 
	 * @param host the host
	 * @return the vms to migrate
	 */
	public abstract Vm getVmToMigrate(IoHost host);

	/**
	 * Gets the migratable vms.
	 * 
	 * @param host the host
	 * @return the migratable vms
	 */
	protected List<IoVm> getMigratableVms(IoHost host) {
		List<IoVm> migratableVms = new ArrayList<IoVm>();
		for (IoVm vm : host.<IoVm> getVmList()) {
			if (!vm.isInMigration()) {
				migratableVms.add(vm);
			}
		}
		return migratableVms;
	}

}
