
     -- excution des scriptes des versements
   
INSERT INTO account_versement(   id, date_versement, montant, name, rib)
    VALUES ((select case when max(id) is null then 0 else  max(id)  end  from account_versement)+1, '2021-09-11', 1000000, 'versement 1', 8);
INSERT INTO account_versement(   id, date_versement, montant, name, rib)
    VALUES ((select case when max(id) is null then 0 else  max(id)  end  from account_versement)+1, '2021-09-12', 2000000, 'versement 2', 8);
INSERT INTO account_versement(   id, date_versement, montant, name, rib)
    VALUES ((select case when max(id) is null then 0 else  max(id)  end  from account_versement)+1, '2021-09-13', 3000000, 'versement 3', 8);
INSERT INTO account_versement(   id, date_versement, montant, name, rib)
    VALUES ((select case when max(id) is null then 0 else  max(id)  end  from account_versement)+1, '2021-09-14', 4000000, 'versement 4', 8);

		/*
		ALTER SEQUENCE account_versement_seq  START WITH ((select case when max(id) is null then 0 else  max(id)  end  from account_versement)+1)

		select  * from account_versement_seq
		select * from  account_versement
		  */
    -- excution des scriptes des recette

 Insert into account_gestion_recette (id, date_recette, montant, rubrique, annee_recette, mois_recette, annexe, rib) 
     values((select case when max(id) is null  then 0 else max(id) end from account_gestion_recette)+1, '2021-09-02', 700000, 305, '2021', '10',1, 8);
 Insert into account_gestion_recette (id, date_recette, montant, rubrique, annee_recette, mois_recette, annexe, rib) 
     values((select case when max(id) is null  then 0 else max(id) end from account_gestion_recette)+1, '2021-09-03', 700000, 305, '2021', '10',1, 8);
 Insert into account_gestion_recette (id, date_recette, montant, rubrique, annee_recette, mois_recette, annexe, rib) 
     values((select case when max(id) is null  then 0 else max(id) end from account_gestion_recette)+1, '2021-09-04', 700000, 305, '2021', '10',1, 8);

     -- excution des scriptes des ordres de payements

insert into purchase_order_payment_commande (id, somme_verement, date_ordre, numero, commande_achat, rubrique_budgetaire )
  Values( ((select case when max(id) is null then 0 else max(id) end from purchase_order_payment_commande)+1), 5000, '2021-09-01', 1996, 16, 283 );
insert into purchase_order_payment_commande (id, somme_verement, date_ordre, numero, commande_achat, rubrique_budgetaire )
  Values( ((select case when max(id) is null then 0 else max(id) end from purchase_order_payment_commande)+1), 5000, '2021-09-01', 1996, 16, 283 );
insert into purchase_order_payment_commande (id, somme_verement, date_ordre, numero, commande_achat, rubrique_budgetaire )
  Values( ((select case when max(id) is null then 0 else max(id) end from purchase_order_payment_commande)+1), 5000, '2021-09-01', 1996, 16, 283 );

   
 
     -- excution des scriptes des ordres de payements
 
     insert into public.hr_ordre_payement_jf (id, archived, import_id, import_origin, version, created_on, updated_on, attrs, beneficiaire_name, beneficiaire_rib, montant, numeroop, objet_paiement, piece_produi_appui, created_by, updated_by, rubrique, datre_ordrejf)
values  
        ((select case when max(id) is null then 0 else max(id) end from  hr_ordre_payement_jf)+1, null, null, null, 0, '2021-12-16 16:33:33.648000', null, null, 'C.N.O.P.S', null, 20236.90, 0, 'COTISATION PATRONALE AUX MUTUELLES DU MOIS Septembre 2021', 'ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE', 1, null, null, '2021-09-1');

         insert into public.hr_ordre_payement_jf (id, archived, import_id, import_origin, version, created_on, updated_on, attrs, beneficiaire_name, beneficiaire_rib, montant, numeroop, objet_paiement, piece_produi_appui, created_by, updated_by, rubrique, datre_ordrejf)
values  
        ((select case when max(id) is null then 0 else max(id) end from  hr_ordre_payement_jf)+1, null, null, null, 0, '2021-12-16 16:33:58.533000', null, null, 'C.N.O.P.S', null, 20236.90, 0, 'COTISATION PATRONALE AUX MUTUELLES DU MOIS Septembre 2021', 'ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE', 1, null, null, '2021-09-11');

 insert into public.hr_ordre_payement_jf (id, archived, import_id, import_origin, version, created_on, updated_on, attrs, beneficiaire_name, beneficiaire_rib, montant, numeroop, objet_paiement, piece_produi_appui, created_by, updated_by, rubrique, datre_ordrejf)
values  
        ((select case when max(id) is null then 0 else max(id) end from  hr_ordre_payement_jf)+1, null, null, null, 0, '2021-12-16 16:34:15.845000', null, null, 'CDG  - CNRA', null, 19000.00, 0, 'COTISATION SALARIALE RECOR DU MOIS Septembre 2021', 'ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE', 1, null, null, '2021-09-12');
insert into public.hr_ordre_payement_jf (id, archived, import_id, import_origin, version, created_on, updated_on, attrs, beneficiaire_name, beneficiaire_rib, montant, numeroop, objet_paiement, piece_produi_appui, created_by, updated_by, rubrique, datre_ordrejf)
values  
        ((select case when max(id) is null then 0 else max(id) end from  hr_ordre_payement_jf)+1, null, null, null, 0, '2021-12-16 16:34:19.847000', null, null, 'M.G.P.A.P', null, 5940.37, 0, 'COTISATION AU MUTUELLE (S.M) ET (C.C.D) DU MOIS Septembre 2021', 'ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE', 1, null, null, '2021-09-13');

   

/*
select * from hr_ordre_payement_jf_seq 
(select case when max(id) is null then 0 else max(id) end from  hr_ordre_payement_jf)+1
select * from public.hr_ordre_payement_jf
 select * from public.hr_ordre_payement_jf
   
*/ 
/*
     select * from purchase_order_payment_commande 

     select case when max(id) is null  then 0 else max(id) end from account_gestion_recette

    select * from account_gestion_recette 
     

    select * from account_versement

    delete from account_versement
 */