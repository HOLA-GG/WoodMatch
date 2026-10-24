insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('woodmatch-photos', 'woodmatch-photos', false, 1048576, array['image/jpeg'])
on conflict (id) do update set public = false, file_size_limit = excluded.file_size_limit,
allowed_mime_types = excluded.allowed_mime_types;
-- No client policies: only the authenticated Edge Function uses the service role.
