class Request < ActiveRecord::Base
  # origin -- submitted by security server
  SECURITY_SERVER = "SECURITY_SERVER"
  # origin -- entered from the central server user interface
  CENTER = "CENTER"

  include Validators
  belongs_to :security_server,
    :class_name => "SecurityServerId",
    :autosave => true,
    :foreign_key => "security_server_id",
    :dependent => :destroy
  belongs_to :sec_serv_user,
    :class_name => "ClientId",
    :autosave => true,
    :foreign_key => "sec_serv_user_id",
    :dependent => :destroy
  belongs_to :request_processing

  # Save the request and perform the corresponding actions.
  # This method can be overriden in child class.
  def register()
    # Verify correctness of the request
    verify_origin()
    verify_request()

    # Execute the action associated with the request
    execute()

    save!
  end

  # Checks that the origin is present and is either CENTER or SECURITY_SERVER
  def verify_origin()
    # TODO: Is translation necessary, since these are essentially programming errors?
    if origin == nil
      raise "Origin must be present"
    end
    if origin != CENTER && origin != SECURITY_SERVER
      raise "Origin must be either #{CENTER} or #{SECURITY_SERVER}"
    end
  end

  # Performs verification specific to the current request.
  # Child classes can override this method.
  def verify_request()
    # No validation is done in the base class
  end

  # Throws exception is security server client with client_id does not exist
  def require_client(client_id)
    if SecurityServerClient.find_by_id(client_id) == nil
      raise I18n.t("requests.client_not_found",
          :client => client_id.to_s)
    end
  end

  # Throws exception is security server with client_id does not exist
  def require_security_server(server_id)
    if SecurityServer.find_server_by_id(server_id) == nil
      raise I18n.t("requests.server_not_found",
          :server => server_id.to_s)
    end
  end

  def from_center?
    origin == CENTER
  end

  # Perform the action associated with the request.
  def execute()
    throw "This method must be reimplemented in a subclass"
  end

  def get_server_owner_name()
    sdsb_member = SdsbMember.find_by_code(
        security_server.member_class, security_server.member_code)

    if !sdsb_member
      return server_owner_name
    end

    sdsb_member.name
  end

  def get_complementary_id()
    complementary_id = ""

    if has_processing?()
      other_request = request_processing.get_other_request(self);
      complementary_id = other_request.id if other_request
    end

    complementary_id
  end

  def get_status()
    has_processing?() ? request_processing.status : ""
  end

  def get_canceling_request_id()
    nil
  end

  def has_processing?
    respond_to?(:request_processing) && request_processing
  end

  # Static database-related methods

  def self.get_requests(query_params, converted_search_params = [])
    logger.info("get_requests('#{query_params}', '#{converted_search_params}'")

    get_search_relation(query_params.search_string, converted_search_params).
        order("#{query_params.sort_column} #{query_params.sort_direction}").
        limit(query_params.display_length).
        offset(query_params.display_start)
  end

  def self.get_request_count(searchable = "", converted_search_params = [])
    searchable.empty? && converted_search_params.empty? ?
        Request.count :
        get_search_relation(searchable, converted_search_params).count
  end

  private

  def self.get_multivalue_search_regex(values)
    # XXX: Assumes that 'type' and 'origin' are always filled in the database.
    return "\s" if values.empty?

    first = true;
    result = "("

    values.each do |each|
      each.strip!

      if first
        first = false
      else
        result << "|"
      end

      result << "#{each}"
    end

    result << ")"
    result
  end

  def self.get_search_relation(searchable, converted_search_params)

    search_params = get_search_sql_params(searchable, converted_search_params)

    # XXX: Is there more elegant way to perform working left join?
    Request.
        where(get_search_sql, *search_params).
        joins("LEFT JOIN request_processings "\
          "ON request_processings.id = requests.request_processing_id").
        joins(:security_server).
        joins(CommonSql::get_identifier_to_member_join_sql)
  end

  def self.get_search_sql
    "CAST(requests.id AS TEXT) LIKE ?
    OR CAST(requests.created_at AS TEXT) LIKE ?
    OR (requests.type) SIMILAR TO ?
    OR (requests.origin) SIMILAR TO ?
    OR lower(security_server_clients.name) LIKE ?
    OR lower(identifiers.member_class) LIKE ?
    OR lower(identifiers.member_code) LIKE ?
    OR lower(identifiers.server_code) LIKE ?
    OR lower(request_processings.status) LIKE ?"
  end

  def self.get_search_sql_params(searchable, converted_params)
    multivalue_regex = get_multivalue_search_regex(converted_params)
    ["%#{searchable}%", "%#{searchable}%","%#{multivalue_regex}%",
        "%#{multivalue_regex}%",  "%#{searchable}%", "%#{searchable}%",
        "%#{searchable}%", "%#{searchable}%", "%#{searchable}%"]
  end

  def self.find_by_server_and_client(clazz, server_id, client_id)
    logger.info("find_by_server_and_client(#{clazz}, #{server_id}, #{client_id})")
    requests = clazz
        .joins(:security_server, :sec_serv_user, :request_processing)
        .where(
          :identifiers => { # association security_server
            :sdsb_instance => server_id.sdsb_instance,
            :member_class => server_id.member_class,
            :member_code => server_id.member_code,
            :server_code => server_id.server_code},
          :sec_serv_users_requests => { # association sec_serv_user
            :sdsb_instance => client_id.sdsb_instance,
            :member_class => client_id.member_class,
            :member_code => client_id.member_code,
            :subsystem_code => client_id.subsystem_code},
          :request_processings => {:status => RequestProcessing::WAITING})

    # Filter for subsystem codes in sec_serv_user because this is cumbersome
    # to do with the SQL query.
    requests.select { |req|
        req.sec_serv_user.subsystem_code == client_id.subsystem_code
    }

    logger.debug("Requests returned: #{requests.inspect}")
    requests
  end

  def self.find_by_server(server_id)
    logger.info("find_by_server(#{server_id})")
    requests = Request.readonly(false)
        .joins(:security_server)
        .where(
          :identifiers => {
            :sdsb_instance => server_id.sdsb_instance,
            :member_class => server_id.member_class,
            :member_code => server_id.member_code,
            :server_code => server_id.server_code})

    logger.debug("Requests returned: #{requests.inspect}")
    requests
  end
end
