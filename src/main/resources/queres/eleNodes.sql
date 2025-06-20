-- select * from v_node_penetration np, v_node n where np.NODE = n.NODE
-- select n.node, n.type from v_node_penetration np, v_node n where np.NODE = n.NODE
-- select np.node, np.type from v_node_penetration np where np.type = 2
select np.node, np.type from v_node_penetration np