select vc.SEQID, vc.CABLE_ID, vc.F_ROUT, vc.FROM_E_ZONE_NAME, vc.FROM_E_ZONE_DESCR, vc.TO_E_ZONE_NAME, vc.TO_E_ZONE_DESCR, vc.FROM_E_ID, vc.FROM_E_DESCR, vc.TO_E_ID, vc.TO_E_DESCR, vc.SEGREGATION, vc.CABLE_SPEC, vc.NOM_SECTION, ROUND(vc.TOTAL_LENGTH, 1), vc.SYSTEM_DESCR, v_cab_data.cab_type_descr FROM v_cable vc left join v_cab_data on vc.CABLE_ID = v_cab_data.CABLE_COD
-- select vc.SEQID, vc.CABLE_ID, vc.F_ROUT FROM v_cable vc left join v_cab_data on vc.CABLE_ID = v_cab_data.CABLE_COD