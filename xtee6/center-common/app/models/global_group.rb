class GlobalGroup < ActiveRecord::Base
  include Validators

  validates :group_code, :unique => true
  has_many :global_group_members, :dependent => :destroy

  # Adds a member to the group. The member should not be saved
  # to database (or loaded from database)
  def add_member(client_id)
    # Check if global group already contains the ID.
    if !has_member?(client_id)
      global_group_members.create!(:group_member => client_id.clean_copy())
    end

    update_member_count()
  end

  # Removes member from the group
  def remove_member(client_id)
    removable_members = GlobalGroupMember.joins(:group_member).where(
        :global_group_id => id,
        :identifiers => client_id_parameters(client_id))

    for to_remove in removable_members
      global_group_members.destroy(to_remove)
    end

    update_member_count()
  end

  # Returns true, if group has member with given ID.
  def has_member?(client_id)
    GlobalGroupMember.joins(:group_member).exists?(
        :global_group_id => id,
        :identifiers => client_id_parameters(client_id))
  end

  # Returns global group that contains security server owners.
  # Returns null, if system parameter is not defined or the
  # group does not exist.
  def self.server_owners_group
    group_code = SystemParameter.server_owners_group
    if group_code == nil
      nil
    else
      first(:conditions => {:group_code => group_code})
    end
  end

  def self.find_by_code(group_code)
     groups = GlobalGroup.where(:group_code => group_code)
     groups.empty? ? nil : groups[0]
  end

  def self.get_groups(query_params)
    get_search_relation(query_params.search_string).
        order("#{query_params.sort_column} #{query_params.sort_direction}").
        limit(query_params.display_length).
        offset(query_params.display_start)
  end

  def self.get_group_count(searchable)
    searchable.empty? ?
        GlobalGroup.count :
        get_search_relation(searchable).count
  end

  def self.add_group(code, description)
    GlobalGroup.create!(:group_code => code, :description => description)
  end

  def self.add_group_if_not_exists(code, description)
    find_by_code(code) == nil ? add_group(code, description) : nil
  end

  def self.update_description(group_id, description)
    group = GlobalGroup.find(group_id)
    group.description = description
    group.save!
  end

  def self.get_member_count(group_id)
    GlobalGroupMember.where(:global_group_id => group_id).count
  end

  private

  def client_id_parameters(client_id)
    { :object_type => client_id.object_type,
      :sdsb_instance => client_id.sdsb_instance,
      :member_class => client_id.member_class,
      :member_code => client_id.member_code,
      :subsystem_code => client_id.subsystem_code }
  end

  def update_member_count
    update_attributes!(:member_count => global_group_members.size)
  end

  def self.get_search_relation(searchable)
    search_params = get_search_sql_params(searchable)
    GlobalGroup.where(get_search_sql, *search_params)
  end

  def self.get_search_sql
    "lower(global_groups.group_code) LIKE ?
    OR lower(global_groups.description) LIKE ?
    OR lower(CAST(global_groups.created_at AS TEXT)) LIKE ?"
  end

  def self.get_search_sql_params(searchable)
    ["%#{searchable}%", "%#{searchable}%", "%#{searchable}%"]
  end
end
