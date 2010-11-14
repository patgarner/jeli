class Task < Sequel::Model
	set_schema do
		primary_key :id
		varchar :title, :unique => true, :empty => false
		boolean :done, :default => false
	end

	create_table unless table_exists?

	if empty?
		create :title => 'Laundry'
		create :title => 'Wash Dishes'
	end
end

