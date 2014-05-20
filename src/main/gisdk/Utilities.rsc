//****************************************************************
//****************************************************************
//Utilities Macros
//
//ModifyOptionsOption
//CloseAll
//IsMapOpen
//IsViewOpen
//SafeDeleteFile
//DeleteFiles
//SafeDeleteFiles
//SafeDeleteDatabase
//NormalizePath
//FormPath
//CreateMapForDatabase
//OpenDatabaseInMap
//OpenDatabase
//OpenRouteSystemInMap
//OpenRouteSystem
//CleanRecordValuesOptionsArray
//FormFieldSpec
//ToString
//GetArrayIndex
//ArraysEqual
//GetDatabaseColumns
//ChooseFileName
//RunProgram
//AddElementToSortedArraySet
//ClearAndDeleteDirectory
//ReadPropertiesFile
//DetokenizePropertyValues
//ComputeAreaBufferOverlayPercentages
//ExportBintoCSV
//ComputeAreaOverlayPercentages
//****************************************************************
//****************************************************************

Macro "ModifyOptionsOption" (options_array,option_key,key,value)
    spec = options_array.(option_key)
    spec.(key) = value
EndMacro

Macro "CloseAll"
    // close all files in workspace
    map_arr=GetMaps()
    if ArrayLength(map_arr)>0 then do
        open_maps=ArrayLength(map_arr[1])
        for mm=1 to open_maps do
            SetMapSaveFlag(map_arr[1][mm],"False")
            CloseMap(map_arr[1][mm])
        end
    end
    
    RunMacro("G30 File Close All")
    
    mtx_arr=GetMatrices()
    if ArrayLength(mtx_arr) > 0 then do
        handle_arr = mtx_arr[1]
        for k = 1 to handle_arr.length do
            handle_arr[k] = null
        end
    end
    
    On NotFound goto no_more_eds
    still_more_eds:
    CloseEditor()
    goto still_more_eds

    no_more_eds:
    On NotFound default

    view_arr=GetViews()
    if ArrayLength(view_arr)>0 then do
        On NotFound goto cont_views
        open_views=ArrayLength(view_arr[1])
        for vv=1 to open_views do
            CloseView(view_arr[1][vv])
            cont_views:
        end
    end
EndMacro

Macro "IsMapOpen" (map)
    maps = GetMapNames()
    for i = 1 to maps.length do
        if maps[i] = map then do
            return(True)
        end
    end
    return(False)
EndMacro

Macro "IsViewOpen" (view)
    views = GetViewNames()
    for i = 1 to views.length do
        if views[i] = view then do
            return(True)
        end
    end
    return(False)
EndMacro

Macro "SafeDeleteFile" (file)
    //just ignores any errors
    if GetFileInfo(file) <> null then do
        On Error goto safe_delete_error
        DeleteFile(file)
        safe_delete_error:
        On Error default
    end
EndMacro

Macro "DeleteFiles" (path)
    files = GetDirectoryInfo(path,"All")
    for i = 1 to files.length do
        DeleteFile(RunMacro("FormPath",{path,files[i][1]}))
    end
EndMacro

Macro "SafeDeleteFiles" (path)
    files = GetDirectoryInfo(path,"All")
    for i = 1 to files.length do
        RunMacro("SafeDeleteFile",RunMacro("FormPath",{path,files[i][1]}))
    end
EndMacro

Macro "SafeDeleteDatabase" (database_file)
    //just ignores any errors
    On Error goto safe_delete_database_error
    On NotFound goto safe_delete_database_error
    DeleteDatabase(file)
    safe_delete_database_error:
    On Error default
    On NotFound default
EndMacro

Macro "NormalizePath" (path)
    if Len(path) > 1 and path[2] = ":" then do
        path = Lower(path[1]) + Right(path,Len(path)-1)
    end
    return(Substitute(path,"/","\\",))
EndMacro

Macro "FormPath" (path_elements)
    if TypeOf(path_elements) <> "array" then do
        ShowMessage("Must form a path out of a list of elements, not: " + TypeOf(path_elements))
        ShowMessage(2)
    end
    //path_elements is an array of elements
    path = ""
    for i = 1 to path_elements.length do
        //change / to \
        p = RunMacro("NormalizePath",path_elements[i])
        if Right(p,1) = "\\" then do
            if Len(p) > 1 then do
                p = Substring(p,1,Len(p)-1)
            end
            else do
                p = ""
            end
        end
        if Left(p,1) = "\\" then do
            if Len(p) > 1 then do
                p = Substring(p,2,Len(p))
            end
            else do
                p = ""
            end
        end
        if path = "" then do
            path = p
        end
        else do
            path = path + "\\" + p
        end
    end
    return(path)
EndMacro

Macro "CreateMapForDatabase" (database_file,map_name)
    linfo=GetDBInfo(database_file)
    scope=linfo[1]
    maps = GetMapNames()
    map_name_not_ok = true
    while map_name_not_ok do
        map_name_not_ok = false
        for i = 1 to maps.length do
            if maps[i] = map_name then do
                map_name = map_name + " "
                map_name_not_ok = true
            end
        end
    end
    map=createMap(map_name,{{"Scope", scope},{"Auto Project","True"}})
    SetMapUnits("Miles")
EndMacro

Macro "OpenDatabaseInMap" (database_file,map)
    info=GetDBInfo(database_file)
    map_made = False
    if map <> null then do
        map_made = RunMacro("IsMapOpen",map)
    end
    else do
        map = info[2]
    end
    if not map_made then do
        RunMacro("CreateMapForDatabase",database_file,map)
    end
    NewLayer=GetDBLayers(database_file)
    layer = AddLayer(map,NewLayer[1],database_file,NewLayer[1])
    if NewLayer.length = 2 then do
        //assumes it is a network file, and hides the nodes and adds the lines
        SetLayerVisibility(map + "|" + layer,"False")
        AddLayer(map,NewLayer[2],database_file,NewLayer[2])
    end
    return(map)
EndMacro

Macro "OpenDatabase" (database_file)
    return(RunMacro("OpenDatabaseInMap",database_file,))
EndMacro

Macro "OpenRouteSystemInMap" (route_system_file,map)
    info = GetRouteSystemInfo(route_system_file)
    map_made = False
    if map <> null then do
        map_made = RunMacro("IsMapOpen",map)
    end
    else do
        map = info[3].Label
    end
    if not map_made then do
        RunMacro("CreateMapForDatabase",info[1],map)
    end
    RunMacro("Set Default RS Style",AddRouteSystemLayer(map,info[3].Label,route_system_file,),"TRUE","FALSE")
    return(map)
EndMacro

Macro "OpenRouteSystem" (route_system_file)
    return(RunMacro("OpenRouteSystemInMap",route_system_file,))
EndMacro

Macro "CleanRecordValuesOptionsArray" (options_array,view_name)
    for i = 1 to options_array.length do
        options_array[i][1] = Substitute(options_array[i][1],view_name + ".","",)
    end
EndMacro

Macro "FormFieldSpec" (view,field)
    //don't think the following is necessary
    //issue_chars = {":"}
    //fix = False
    //for i = 1 to issue_chars.length do
    //    if Position(field,issue_chars[i]) > 0 then do
    //        fix = True
    //    end
    //end
    //if fix then do
    //    field = "[" + field + "]"
    //end
    return(view + "." + field)
EndMacro

Macro "ToString" (value)
    type = TypeOf(value)
    if type = "string" then do
        return(value)
    end
    else if type = "int" then do
        return(i2s(value))
    end
    else if type = "double" then do
        return(r2s(value))
    end
    else if type = "null" then do
        return("")
    end
    ShowMessage("Type " + type + " not supported by ToString method")
EndMacro

Macro "GetArrayIndex" (array,value)
    //returns the index of value in array, or 0 if it is not found
    type = TypeOf(value)
    for i = 1 to array.length do
        if TypeOf(array[i]) = type and array[i] = value then do
            return(i)
        end
    end
    return(0)
EndMacro

Macro "ArraysEqual" (array1,array2)
    if array1.length <> array2.length then do
        return(False)
    end
    for i = 1 to array1.length do
        if TypeOf(array1[i]) = "array" then do
            if TypeOf(array2[i]) = "array" then do
                if not RunMacro("ArraysEqual",array1[i],array2[i]) then do
                    return(False)
                end
            end
            else do
                return(False)
            end
        end
        else if TypeOf(array2[i]) = "array" then do
            return(False)
        end
        else do
            if array1[i] <> array2[i] then do
                return(False)
            end
        end
    end
    return(True)
EndMacro

Macro "GetDatabaseColumns" (database_file,layer_name)
    columns = null
    if database_file <> null and GetFileInfo(database_file) <> null then do
        current_layer = GetLayer()
        current_view = GetView()
        lyr = AddLayerToWorkspace("__temp__",database_file,layer_name,{{"Shared","True"}}) 
        layer_in_use = lyr <> "__temp__"
        SetLayer(lyr)
        v = GetView()
        info = GetTableStructure(v)
        for i = 1 to info.length do
            columns = columns + {info[i][1]}
        end
        if not layer_in_use then do
            DropLayerFromWorkspace(lyr)
        end
        if current_layer <> null then do
            SetLayer(current_layer)
        end
        if current_view <> null then do
            SetView(current_view)
        end
    end
    return(columns)
EndMacro

//same as built in TC function, but with error checking for escape and for if a file is in use
Macro "ChooseFileName" (file_types,title,options)
    on escape do 
        fname = null
        goto cfn_done
    end
    openfile:
    fname = ChooseFileName(file_types,title,options)
    if FileCheckUsage({fname},) then do
        ShowMessage("File already in use.  Please choose again.")
        goto openfile
    end
    cfn_done:
    on escape default
    return(fname)
EndMacro

Macro "RunProgram" (program_with_arguments,working_directory) //can't get output file to work right now..boo hoo
    wd = ""
    if working_directory <> null then do
        wd = " /D" + working_directory
    end
    RunProgram("cmd /s /c \"start \"cmd\" " + wd + " /WAIT " + program_with_arguments + "\"",)
EndMacro

Macro "AddElementToSortedArraySet" (array,element)
    index = array.length + 1
    not_done = True
    for i = 1 to array.length do
        if not_done then do
            if element = array[i] then do
                index = -1
                not_done = False
            end
            else if element < array[i] then do
                index = i
                not_done = False
            end
        end
    end
    if index > 0 then do
        array = InsertArrayElements(array,index,{element})
    end
    return(array)
EndMacro

Macro "ClearAndDeleteDirectory" (path)
    //this doesn't do any error handling
    info = GetDirectoryInfo(RunMacro("FormPath",{path,"*"}),"All")
    for i = 1 to info.length do
        f = RunMacro("FormPath",{path,info[i][1]})
        if info[i][2] = "file" then do
            DeleteFile(f)
        end
        else if info[i][2] = "directory" then do
            RunMacro("ClearAndDeleteDirectory",f)
        end
    end
    RemoveDirectory(path)
EndMacro

Macro "ReadPropertiesFile" (properties_file)
    props = null
    f = OpenFile(properties_file,"r")
    while not FileAtEOF(f) do
        line = Trim(ReadLine(f))
        if Len(line) > 0 then do
            subs = ParseString(line,"=", {{"Include Empty",True}})
            key = subs[1]
            value = JoinStrings(Subarray(subs,2,subs.length-1),"=")
            props.(Trim(key)) = Trim(value)
        end
    end
    CloseFile(f)
    return(props)
EndMacro

Macro "DetokenizePropertyValues" (properties,token_map)
    for i = 1 to properties.length do
        value = token_map[i][2]
        for j = 1 to token_map.length do
            value = Substitute(value,token_map[i][1],token_map[i][2],)
        end
        token_map[i][2] = value
    end
EndMacro

Macro "ComputeAreaBufferOverlayPercentages" (area_layer_file,centroid_layer_file,centroid_query,area_taz_field,node_taz_field,buffer_size)
    //assumes node layer holds centroids from area layer, and bases its buffer around this
    //returns array of percentage arrays, each holding {centroid_taz,overlay_taz,percentage}
    omap_name = GetMap()
    olayer_name = GetLayer()
    oview_name = GetView()
    
    map = RunMacro("OpenDatabase",area_layer_file)
    RunMacro("OpenDatabaseInMap",centroid_layer_file,map)
    node_layer = GetMapLayers(map,"Point")
    node_layer = node_layer[1][1]
    area_layer = GetMapLayers(map,"Area")
    area_layer = area_layer[1][1]
    
    SetLayer(node_layer)
    centroid_selection = "centroids"
    SelectByQuery(centroid_selection,"Several",centroid_query)
    node_ids = GetSetIDs(node_layer + "|" + centroid_selection)
    node_to_taz = null
    for i = 1 to node_ids.length do
        node_id = node_ids[i]
        value = GetRecordValues(node_layer,IDToRecordHandle(node_id),{node_taz_field})
        node_to_taz.(i2s(node_id)) = value[1][2]
    end
    
    percentages = null
    temp_dir = GetFileInfo(area_layer_file)
    temp_dir = Substring(area_layer_file,1,Len(area_layer_file) - Len(temp_dir[1]))
    intersection_file = "temp_buffers.dbd"
    percentages_file = "tempintersect"
    temp_intersection_file = RunMacro("FormPath",{temp_dir,intersection_file})
    temp_percentages_file = RunMacro("FormPath",{temp_dir,percentages_file})
    EnableProgressBar("Calculating Area Buffer Percentages (buffer = " + r2s(buffer_size) + ")", 1)     // Allow only a single progress bar
    CreateProgressBar("", "True")

    nlen = node_ids.length
    //for i = 1 to nlen do
    for i = 1 to 20 do
        node_id = node_ids[i]
        node_taz = node_to_taz.(i2s(node_id))
        stat = UpdateProgressBar("Zone: " + i2s(node_taz), r2i(i/nlen*100))
        if stat = "True" then do
            percentages = null
            goto quit_loop
        end
        SetLayer(node_layer)
        SelectByQuery("centroid","Several","SELECT * WHERE id=" + i2s(node_id))
        CreateBuffers(temp_intersection_file,"buffers",{"centroid"},"Value",{buffer_size},{{"Interior","Separate"},{"Exterior","Separate"}})
    
        NewLayer = GetDBLayers(temp_intersection_file)
        intersection_layer = AddLayer(map,"inter",temp_intersection_file,NewLayer[1])
        SetLayer(area_layer)
        n = SelectByVicinity("subtaz","several",node_layer+"|centroid",buffer_size,{{"Inclusion","Intersecting"}})
        if n > 0 then do
            ComputeIntersectionPercentages({intersection_layer, area_layer + "|subtaz"}, temp_percentages_file + ".bin",)
            t = OpenTable("int_table", "FFB", {temp_percentages_file + ".bin"},)
            tbar = t + "|"
            rh = GetFirstRecord(tbar,)
            while rh <> null do
                vals = GetRecordValues(t,rh,{"Area_1", "Area_2","Percent_2"})
                if vals[1][2] = 1 and vals[2][2] <> 0 then do
                    value = GetRecordValues(area_layer,IDToRecordHandle(vals[2][2]),{area_taz_field})
                    area_taz = node_to_taz.(i2s(value[1][2]))
                    percentages = percentages + {{node_taz,area_taz,vals[3][2]}}
                end
                rh = GetNextRecord(t+"|",,)
            end
            CloseView(t)
        end
        DropLayer(map,intersection_layer)
    end
    
    quit_loop:
    DestroyProgressBar()
    CloseMap(map)
    if omap_name <> null then do
        SetMap(omap_name)
        if olayer_name <> null then do
            SetLayer(olayer_name)
        end
    end
    if oview_name <> null then do
        SetView(oview_name)
    end
    DeleteDatabase(temp_intersection_file)
    DeleteFile(temp_percentages_file + ".bin")
    DeleteFile(temp_percentages_file + ".BX")
    DeleteFile(temp_percentages_file + ".dcb")
    
    return(percentages)
EndMacro

Macro "ExportBintoCSV"(input_file_base, output_file_base)

  view = OpenTable("Binary Table","FFB",{input_file_base+".bin",}, {{"Shared", "True"}})
  SetView(view)
  ExportView(view+"|", "CSV", output_file_base+".csv",,{{"CSV Header", "True"}})
  CloseView(view)
  ok=1
  quit:
    return(ok)
EndMacro


Macro "ComputeAreaOverlayPercentages" (area_layer_file,overlay_layer_file,area_id_field,overlay_id_field)
    //returns percentage array, each element holding {area_id,overlay_id,% of overlay in area}
    omap_name = GetMap()
    olayer_name = GetLayer()
    oview_name = GetView()
    
    map = RunMacro("OpenDatabase",area_layer_file)
    area_layer = GetMapLayers(map,"Area")
    area_layer = area_layer[1][1]
    RunMacro("OpenDatabaseInMap",overlay_layer_file,map)
    overlay_layer = GetMapLayers(map,"Area")
    if overlay_layer[1][1] = area_layer then do
        overlay_layer = overlay_layer[1][2]
    end
    else do
        overlay_layer = overlay_layer[1][1]
    end
    
    area_ids = GetSetIDs(area_layer + "|")
    
    percentages = null
    temp_dir = GetFileInfo(area_layer_file)
    temp_dir = Substring(area_layer_file,1,Len(area_layer_file) - Len(temp_dir[1]))
    percentages_file = "tempintersect"
    temp_percentages_file = RunMacro("FormPath",{temp_dir,percentages_file})
    EnableProgressBar("Calculating Area Intersections", 1)     // Allow only a single progress bar
    CreateProgressBar("", "True")

    nlen = area_ids.length
    for i = 1 to nlen do
        area_id = area_ids[i]
        stat = UpdateProgressBar("Area id: " + i2s(area_id), r2i(i/nlen*100))
        if stat = "True" then do
            percentages = null
            goto quit_loop
        end
        SetLayer(area_layer)
        SelectByQuery("select","Several","SELECT * WHERE id=" + i2s(area_id))
        area_sid = GetRecordValues(area_layer,IDToRecordHandle(area_id),{area_id_field})
        area_sid = area_sid[1][2]
        SetLayer(overlay_layer)
        n = SelectByVicinity("subtaz","several",area_layer+"|select",0,{{"Inclusion","Intersecting"}})
        if n > 0 then do
            ComputeIntersectionPercentages({area_layer+"|select",overlay_layer + "|subtaz"}, temp_percentages_file + ".bin",)
            t = OpenTable("int_table", "FFB", {temp_percentages_file + ".bin"},)
            tbar = t + "|"
            rh = GetFirstRecord(tbar,)
            while rh <> null do
                vals = GetRecordValues(t,rh,{"Area_1", "Area_2","Percent_2"})
                if vals[1][2] > 0 and vals[2][2] <> 0 and vals[3][2] > 0.0 then do
                    value = GetRecordValues(overlay_layer,IDToRecordHandle(vals[2][2]),{overlay_id_field})
                    percentages = percentages + {{area_sid,value[1][2],vals[3][2]}}
                end
                rh = GetNextRecord(t+"|",,)
            end
            CloseView(t)
        end
    end
    
    quit_loop:
    DestroyProgressBar()
    CloseMap(map)
    if omap_name <> null then do
        SetMap(omap_name)
        if olayer_name <> null then do
            SetLayer(olayer_name)
        end
    end
    if oview_name <> null then do
        SetView(oview_name)
    end
    DeleteFile(temp_percentages_file + ".bin")
    DeleteFile(temp_percentages_file + ".BX")
    DeleteFile(temp_percentages_file + ".dcb")
    
    return(percentages)
EndMacro

//****************************************************************
//****************************************************************
//SandagCommon Macros
//
//ExportMatrixToCSV
//ExportMatrix
//MatrixSize
//CreateMatrix
//Aggregate Matrices
//GoGetMatrixCoreNames
//GetSLQuery#
//CloseViews
//DateAndTime
//SafeCopyFile
//SafeRenameFIle
//HwycadLog
//ForecastYearStr
//ForecastYearInt
//DeleteInterimFiles
//FileCheckDelete
//GetPathDirectory
//****************************************************************
//****************************************************************

Macro "ExportMatrixToCSV" (path,filename,corename,filenameout)
    m = OpenMatrix(path+"\\"+filename, "True")
    mc = CreateMatrixCurrency(m,corename,,,)
    rows = GetMatrixRowLabels(mc)
    ExportMatrix(mc, rows, "Rows", "CSV", path+"\\"+filenameout, )
    return(1)
EndMacro

Macro "ExportMatrix" (path,filename,corename,filenameout,outputtype)
    //path as string - path="T:\\transnet2\\devel\\sr12\\sr12_byear\\byear"
    //filename as string - must be a matrix - filename="SLAgg.mtx"
    //corename as string - corename="DAN"
    //filenameout as string - filenameout="SLAgg.csv"
    //outputtype as string - ("dBASE", "FFA", "FFB" or "CSV")
    
    m = OpenMatrix(path+"\\"+filename, "True")
    mc = CreateMatrixCurrency(m,corename,,,)
    rows = GetMatrixRowLabels(mc)
    ExportMatrix(mc, rows, "Rows", outputtype, path+"\\"+filenameout, )
    return(1)
EndMacro

Macro "MatrixSize" (path, filename, corename)
  //gets the size (number of zones) in the matrix - useful for sr11 vs sr12 and for split zones
  m = OpenMatrix(path+"\\"+filename, "True")
  base_indicies = GetMatrixBaseIndex(m)
  mc = CreateMatrixCurrency(m, corename, base_indicies[1], base_indicies[2], )
  v = GetMatrixVector(mc, {{"Marginal", "Row Count"}})
  vcount = VectorStatistic(v, "Count", )
  return(vcount)
EndMacro

Macro "CreateMatrix" (path, filename, label, corenames, zone)
  Opts = null
  Opts.[File Name] = (path+"\\"+filename)
  Opts.Label = label
  Opts.Type = "Float"
  Opts.Tables = corenames
  Opts.[Column Major] = "No"
  Opts.[File Based] = "Yes"
  Opts.Compression = 0
  m = CreateMatrixFromScratch(label, zone, zone, Opts)
  return(1)
EndMacro

Macro "AggregateMatrices" (path, xref, xrefcol1, xrefcol2, mtx, corenm, aggmtx)
  // Aggregate Matrix Options
  m = OpenMatrix(path+"\\"+mtx, "True")
  base_indicies = GetMatrixBaseIndex(m)
  Opts = null
  Opts.Input.[Matrix Currency] = {path+"\\"+mtx, corenm, base_indicies[1], base_indicies[2]}
  Opts.Input.[Aggregation View] = {xref, "xref"}
  Opts.Global.[Row Names] = {"xref."+xrefcol1, "xref."+xrefcol2}
  Opts.Global.[Column Names] = {"xref."+xrefcol1, "xref."+xrefcol2}
  Opts.Output.[Aggregated Matrix].Label = "AggMtx"+"_"+corenm
  Opts.Output.[Aggregated Matrix].Compression = 1
  Opts.Output.[Aggregated Matrix].[File Name] = path+"\\"+aggmtx

  ok = RunMacro("TCB Run Operation", 1, "Aggregate Matrix", Opts, )
  return(ok)
EndMacro

Macro "GoGetMatrixCoreNames" (path, matrix)
  m = OpenMatrix(path+"\\"+matrix, )
  core_names=GetMatrixCoreNames(m)
  return(core_names)
EndMacro

Macro "GetSLQuery#" (path)
  //Modified from "Prepare queries for select link analysis, by JXu on Nov 29, 2006"
  selinkqry_file="\\selectlink_query.txt"
  fptr_from = OpenFile(path + selinkqry_file, "r")
  tmp_qry=readarray(fptr_from)
  index =1
  query=0
  selinkqry_name=null
  selink_qry=null
  subs=null
  while index <=ArrayLength(tmp_qry) do
    if left(trim(tmp_qry[index]),1)!="*" then do
      query=query+1
    end
    index = index + 1
  end
  return(query)
EndMacro

Macro "CloseViews" 
    vws = GetViewNames()
    for i = 1 to vws.length do
  	  CloseView(vws[i])
  	end
EndMacro

// returns a nicely formatted day and time
Macro "DateAndTime"
  date_arr = ParseString(GetDateAndTime(), " ")
  day = date_arr[1]
  mth = date_arr[2]
  num = date_arr[3]
  time = Left(date_arr[4], StringLength(date_arr[4])-3)
  year = SubString(date_arr[5], 1, 4)
  today = mth + "/" + num + "/" + year + " " + time
  //showmessage(today) 
  Return(today)
EndMacro

Macro "SafeCopyFile"(arr)
  file1=arr[1]
  file2=arr[2]
  dif2=GetDirectoryInfo(file2,"file") 
  if dif2.length>0 then deletefile(file2)
  dif2=GetDirectoryInfo(file1,"file") 
  if dif2.length>0 then copyfile(file1,file2) 
  ok=1
  quit:
    return(ok)
EndMacro

Macro "SafeRenameFile"(arr)
  file1=arr[1]
  file2=arr[2]
  dif1=GetDirectoryInfo(file2,"file") 
  if dif1.length>0 then deletefile(file2)
  dif2=GetDirectoryInfo(file1,"file") 
  if dif2.length>0 then RenameFile(file1, file2)  
  ok=1
  quit:
    return(ok)    
EndMacro

Macro "HwycadLog"(arr)
  shared path
  fprlog=null
  log1=arr[1]
  log2=arr[2]
  dif2=GetDirectoryInfo(path+"\\hwycadx.log","file")
  if dif2.length>0 then fprlog=OpenFile(path+"\\hwycadx.log","a") 
  else fprlog=OpenFile(path+"\\hwycadx.log","w")
  mytime=GetDateAndTime() 
  writeline(fprlog,mytime+", "+log1+", "+log2)
  CloseFile(fprlog)
  fprlog = null
  return()
EndMacro

Macro "ForecastYearStr"
  shared path_study,path
  fptr = OpenFile(path+"\\year", "r")
   strYear = ReadLine(fptr)
  closefile(fptr)
  return(strYear)
EndMacro

Macro "ForecastYearInt"
  //usage: myyear=RunMacro("ForecastYearInt")
  shared path_study,path
  fptr = OpenFile(path+"\\year", "r")
   strFyear = ReadLine(fptr)
  closefile(fptr)
  intFyear=S2I(strFyear)
  return(intFyear)
EndMacro

Macro "DeleteInterimFiles" (path, FileNameArray,RscName,MacroName,FileDescription)
  RunMacro("HwycadLog",{RscName+": "+MacroName,"SafeDeleteFile, "+FileDescription})
  for i = 1 to FileNameArray.length do //delete existing files
    ok=RunMacro("SafeDeleteFile",path+"\\"+FileNameArray[i]) if !ok then goto quit
  end
  quit:
    return(ok)
EndMacro

Macro "FileCheckDelete" (path,filename)
  //usage: RunMacro("FileCheckDelete",path,filename) where path and filename are strings
  di = GetDirectoryInfo(path+"\\"+filename, "File")
  if di.length > 0 then do
    ok=RunMacro("SDdeletefile",{path+"\\"+filename}) 
    return(ok)
  end
EndMacro

// Macro "getpathdirectory" doesn't allow the selected path with different path_study.
Macro "GetPathDirectory"
  shared path,path_study,scr
  opts={{"Initial Directory", path_study}}
  tmp_path=choosedirectory("Choose an alternative directory in the same study area", opts)
  strlen=len(tmp_path)                
  for i = 1 to strlen do
    tmp=right(tmp_path,i)
    tmpx=left(tmp,1)
    if tmpx="\\" then goto endfor 
  end 
  endfor:
    strlenx=strlen-i
    tmppath_study=left(tmp_path,strlenx)
    if path_study=tmppath_study then do
      path=tmp_path
      tmp_flag=0
      for i=1 to scr.length do
        if scr[i]=path then do
          tmp_flag=1
          i=scr.length+1
        end
        else i=i+1
      end
      if tmp_flag=0 then do
        tmp = CopyArray(scr)
        tmp = tmp + {tmp_path}
        scr = CopyArray(tmp)
      end
      //showmessage("write description of the alternative in the head file")
      //x=RunProgram("notepad "+path+"\\head",)
      mytime=GetDateAndTime()
      fptr=openfile(path+"\\tplog","a")
      WriteLine(fptr, mytime)
      closefile(fptr)
      //showmessage("type in the reason why you are doing the model run in tplog")    
      //x=RunProgram("notepad "+path+"\\tplog",)
    end
    else do
      path=null
      msg1="The alternative directory selected is invalid because it has different study area! "
      msg2="Please select again within the same study area " 
      msg3=" or use the Browse button to select a different study area."
      showMessage(msg1+msg2+path_study+msg3)
    end
EndMacro


