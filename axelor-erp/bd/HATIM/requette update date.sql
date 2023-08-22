UPDATE configuration_gestion_salaire
set indemnite_de_hierarchie_administrative = indemnite_de_technicite
where indemnite_de_technicite is not null;

update hr_employee
set marriage_date = '2021-01-01'
where marital_status = 1;


update hr_naissances
set date_naiss = '2021-09-02'
where 1=1;

update hr_historique_responsabilite
set etat = 'En cours'
where 1=1;

update hr_employee
set daterecrutement = '2021-01-01'
where 1=1