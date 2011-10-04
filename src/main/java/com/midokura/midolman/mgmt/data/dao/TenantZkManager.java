/*
 * @(#)TenantZkManager        1.6 19/09/08
 *
 * Copyright 2011 Midokura KK
 */
package com.midokura.midolman.mgmt.data.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.ZooDefs.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.midokura.midolman.state.Directory;
import com.midokura.midolman.state.StateAccessException;
import com.midokura.midolman.state.ZkManager;
import com.midokura.midolman.state.ZkStateSerializationException;

/**
 * Abstract base class for TenantZkManager.
 * 
 * @version 1.6 19 Sept 2011
 * @author Ryu Ishimoto
 */
public class TenantZkManager extends ZkManager {

    private ZkMgmtPathManager zkMgmtPathManager = null;
    private final static Logger log = LoggerFactory
            .getLogger(TenantZkManager.class);

    /**
     * TenantZkManager constructor.
     * 
     * @param zk
     *            ZooKeeper object.
     * @param basePath
     *            Directory to set as the base.
     */
    public TenantZkManager(Directory zk, String basePath, String mgmtBasePath) {
        super(zk, basePath);
        zkMgmtPathManager = new ZkMgmtPathManager(mgmtBasePath);
    }

    public void delete(UUID id) throws StateAccessException,
            ZkStateSerializationException, UnsupportedOperationException {
        List<Op> ops = new ArrayList<Op>();
        Set<String> routers = getChildren(
                zkMgmtPathManager.getTenantRoutersPath(id), null);
        RouterZkManagerProxy routerManager = new RouterZkManagerProxy(zk,
                pathManager.getBasePath(), zkMgmtPathManager.getBasePath());
        for (String router : routers) {
            ops.addAll(routerManager.prepareRouterDelete(UUID
                    .fromString(router)));
        }

        Set<String> bridges = getChildren(
                zkMgmtPathManager.getTenantBridgesPath(id), null);
        BridgeZkManagerProxy bridgeManager = new BridgeZkManagerProxy(zk,
                pathManager.getBasePath(), zkMgmtPathManager.getBasePath());
        for (String bridge : bridges) {
            ops.addAll(bridgeManager.prepareDelete(UUID.fromString(bridge)));
        }

        String path = zkMgmtPathManager.getTenantBridgeNamesPath(id);
        log.debug("Preparing to delete: " + path);
        ops.add(Op.delete(path, -1));
        path = zkMgmtPathManager.getTenantRouterNamesPath(id);
        log.debug("Preparing to delete: " + path);
        ops.add(Op.delete(path, -1));
        path = zkMgmtPathManager.getTenantBridgesPath(id);
        log.debug("Preparing to delete: " + path);
        ops.add(Op.delete(path, -1));
        path = zkMgmtPathManager.getTenantRoutersPath(id);
        log.debug("Preparing to delete: " + path);
        ops.add(Op.delete(path, -1));
        path = zkMgmtPathManager.getTenantPath(id);
        log.debug("Preparing to delete: " + path);
        ops.add(Op.delete(path, -1));
        multi(ops);
    }
    
    /**
     * Add a new tenant entry in the ZooKeeper directory.
     * 
     * 
     * @param tenant
     *            TenantConfig object to store tenant data.
     * @throws KeeperException
     *             General ZooKeeper exception.
     * @throws InterruptedException
     *             Unresponsive thread getting interrupted by another thread.
     */
    public UUID create() throws StateAccessException {
        return create(null);
    }

    /**
     * Add a new tenant entry in the ZooKeeper directory.
     * 
     * @param id
     *            Tenant UUID
     * @param tenant
     *            TenantConfig object to store tenant data.
     * @throws KeeperException
     *             General ZooKeeper exception.
     * @throws InterruptedException
     *             Unresponsive thread getting interrupted by another thread.
     */
    public UUID create(UUID id) throws StateAccessException {
        if (null == id) {
            id = UUID.randomUUID();
        }
        List<Op> ops = new ArrayList<Op>();
        ops.add(Op.create(zkMgmtPathManager.getTenantPath(id), null,
                Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
        ops.add(Op.create(zkMgmtPathManager.getTenantRoutersPath(id), null,
                Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
        ops.add(Op.create(zkMgmtPathManager.getTenantBridgesPath(id), null,
                Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
        ops.add(Op.create(zkMgmtPathManager.getTenantRouterNamesPath(id), null,
                Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
        ops.add(Op.create(zkMgmtPathManager.getTenantBridgeNamesPath(id), null,
                Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
        multi(ops);
        return id;
    }

}
