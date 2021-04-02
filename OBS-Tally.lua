obs = obslua
--local http = require("socket.http")
--local ltn12 = require("ltn12")
--local mime = require("mime")
settings = {}

local username = "<username>"
local password = "<password>"

-- Script hook for defining the script description
function script_description()
	return [[
Sends updates on active and preview scenes to a RabbitMQ exchange called 'cam' on default Vhost
]]
end

-- Script hook for defining the settings that can be configured for the script
function script_properties()
	local props = obs.obs_properties_create()

	obs.obs_properties_add_text(props, "endpoint", "Endpoint", obs.OBS_TEXT_DEFAULT)

	local scenes = obs.obs_frontend_get_scenes()

	if scenes ~= nil then
		for _, scene in ipairs(scenes) do
			local scene_name = obs.obs_source_get_name(scene)
			obs.obs_properties_add_bool(props, "scene_enabled_" .. scene_name, "Execute when '" .. scene_name .. "' is activated")
			obs.obs_properties_add_text(props, "scene_value_" .. scene_name, scene_name .. " value", obs.OBS_TEXT_DEFAULT)
		end
	end

	obs.source_list_release(scenes)

	return props
end

-- Script hook that is called whenver the script settings change
function script_update(_settings)
	settings = _settings
end

-- Script hook that is called when the script is loaded
function script_load(settings)
	obs.obs_frontend_add_event_callback(handle_event)
end

function handle_event(event)
	if event == obs.OBS_FRONTEND_EVENT_SCENE_CHANGED then
		handle_scene_change()
	elseif event == obs.OBS_FRONTEND_EVENT_PREVIEW_SCENE_CHANGED then
		handle_preview_scene_change()
	end
end

function handle_scene_change()
	local scene = obs.obs_frontend_get_current_scene()
	local scene_name = obs.obs_source_get_name(scene)
	local scene_enabled = obs.obs_data_get_bool(settings, "scene_enabled_" .. scene_name)
	if scene_enabled then
		local scene_value = obs.obs_data_get_string(settings, "scene_value_" .. scene_name)
		obs.script_log(obs.LOG_INFO, "Activating " .. scene_name .. ". Sending message:\n  " .. scene_value)
		send_message(scene_value)
	else
		obs.script_log(obs.LOG_INFO, "Activating " .. scene_name .. ". Message is disabled for this scene.")
	end
	obs.obs_source_release(scene);
end

function handle_preview_scene_change()
	local scene = obs.obs_frontend_get_current_preview_scene()
	local scene_name = obs.obs_source_get_name(scene)
	local scene_enabled = obs.obs_data_get_bool(settings, "scene_enabled_" .. scene_name)
	if scene_enabled then
		local scene_value = "p_" .. obs.obs_data_get_string(settings, "scene_value_" .. scene_name)
		obs.script_log(obs.LOG_INFO, "Previewing " .. scene_name .. ". Sending message:\n  " .. scene_value)
		send_message(scene_value)
	else
		obs.script_log(obs.LOG_INFO, "Previewing " .. scene_name .. ". Message is disabled for this scene.")
	end
	obs.obs_source_release(scene);
end

function send_message(message)
	local endpoint = obs.obs_data_get_string(settings, "endpoint")
--    local response_body = { }
--    local res, code, response_headers, status = http.request {
--        url = path,
--        method = "POST",
--        headers = {
--            ["Authentication"] = "Basic " .. mime.b64(username .. ":" .. password),
--            ["Content-Type"] = "application/json",
--            ["Content-Length"] = body:len()
--        },
--        source = ltn12.source.string(body),
--        sink = ltn12.sink.table(response_body)
--    }
    local command = [[curl -i -u ]] .. username .. ":" .. password .. [[ -H "content-type:application/json" -X POST http://]] .. endpoint .. [[:15672/api/exchanges/%2f/cam/publish -d '{"properties":{},"routing_key":"","payload":"]] .. message .. [[","payload_encoding":"string"}']]
    local res = os.execute(command)
end
