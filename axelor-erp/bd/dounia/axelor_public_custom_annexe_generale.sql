

DROP TABLE custom_annexe_generale

INSERT INTO public.custom_annexe_generale(
	id, archived, import_id, import_origin, version, created_on, updated_on, attrs, name, created_by, updated_by, adresse, telephone)
	VALUES (1, null, null, null, null, null, null, null, 'Siège', null, null, 'Bd Abdellah Chefchaouni, 30000, Fès, Maroc', '+212535623183'),
	
	(2, null, null, null, null, null, null, null, 'Meknès', null, null, 'Place Abdelaziz Ben Driss, 50000, Meknès, Maroc', '+212535510937'),
	
	(3, null, null, null, null, null, null, null, 'Taza', null, null, 'Boulevard Hassan II, 35000, Taza, Maroc', '+212535673583'),
	
	(4, null, null, null, null, null, null, null, 'El Hajeb', null, null, 'Boulevard Hassan 2, Hajeb bas, El Hajeb, Maroc', '+212535541563');