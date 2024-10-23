-- select * from v_node_penetration np, v_node n where np.NODE = n.NODE
select n.node, n.type from v_node_penetration np, v_node n where np.NODE = n.NODE