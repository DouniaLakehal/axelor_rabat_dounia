delete from purchase_offre_appel_societe
delete from purchase_sociecte
delete from purchase_statut
    
insert into public.purchase_statut (id, archived, import_id, import_origin, version, created_on, updated_on, attrs, nom, created_by, updated_by)
values  (1, null, null, null, null, null, null, null, 'Accepter', null, null),
        (2, null, null, null, null, null, null, null, 'Eliminer', null, null);