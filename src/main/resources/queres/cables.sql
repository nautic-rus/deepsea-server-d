select vc.SEQID, vc.CABLE_ID, vc.F_ROUT, vc.CABLE_SPEC, vc.FROM_E_ZONE_NAME, vc.FROM_E_ZONE_DESCR, vc.TO_E_ZONE_NAME, vc.TO_E_ZONE_DESCR, vc.FROM_E_ID, vc.FROM_E_DESCR, vc.TO_E_ID, vc.TO_E_DESCR, vc.SEGREGATION, vc.CABLE_SPEC, vc.NOM_SECTION, vc.TOTAL_LENGTH, vc.SYSTEM_DESCR, v_cab_data.cab_type_descr FROM v_cable vc left join v_cab_data on vc.CABLE_ID = v_cab_data.CABLE_COD