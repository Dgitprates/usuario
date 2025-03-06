package com.diego.usuario.business;

import com.diego.usuario.business.converter.UsuarioConverter;
import com.diego.usuario.business.dto.EnderecoDTO;
import com.diego.usuario.business.dto.TelefoneDTO;
import com.diego.usuario.business.dto.UsuarioDTO;
import com.diego.usuario.infrastructure.entity.Endereco;
import com.diego.usuario.infrastructure.entity.Telefone;
import com.diego.usuario.infrastructure.entity.Usuario;
import com.diego.usuario.infrastructure.exceptions.ConflictException;
import com.diego.usuario.infrastructure.exceptions.ResourceNotFoundException;
import com.diego.usuario.infrastructure.repository.EnderecoRepository;
import com.diego.usuario.infrastructure.repository.TelefoneRepository;
import com.diego.usuario.infrastructure.repository.UsuarioRepository;
import com.diego.usuario.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioConverter usuarioConverter;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EnderecoRepository enderecoRepository;
    private final TelefoneRepository telefoneRepository;

    public UsuarioDTO salvaUsuario(UsuarioDTO usuarioDTO){
        emailExiste(usuarioDTO.getEmail());
        usuarioDTO.setSenha(passwordEncoder.encode(usuarioDTO.getSenha()));
        Usuario usuario = usuarioConverter.paraUsuario(usuarioDTO);
        usuario = usuarioRepository.save(usuario);
        return usuarioConverter.paraUsuarioDTO(usuario);
    }

    public void emailExiste(String email){
        try {
            boolean existe = verificaEmailExistente(email);
            if (existe){
                throw new ConflictException("Email já cadastrado " + email);
            }
        }catch (ConflictException e) {
            throw new ConflictException("Email já cadastrado " + e.getCause());
        }
    }

    public boolean verificaEmailExistente(String email){
        return usuarioRepository.existsByEmail(email);
    }

    public UsuarioDTO buscarUsuarioPorEmail(String email) {
        try {
            return usuarioConverter.paraUsuarioDTO(usuarioRepository.findByEmail(email).orElseThrow(
                    () -> new ResourceNotFoundException("Email não encontrado" + email)));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Email não encontrado " + email);
        }
    }

    public void deletaUsuarioPorEmail(String email){
        usuarioRepository.deleteByEmail(email);
    }

    public UsuarioDTO atualizaDadosUsuario(String token, UsuarioDTO dto){
        //Busca o email do usuário através do token (Tira a obrigatoriedade do email)
        String email = jwtUtil.extrairEmailToken(token.substring(7));

        //Criptografa a senha
        dto.setSenha(dto.getSenha() != null ? passwordEncoder.encode(dto.getSenha()) : null);

        //Busca os dados o usuário no banco
        Usuario usuarioEntity = usuarioRepository.findByEmail(email).orElseThrow(() ->
                new ResourceNotFoundException("Email não encontrado"));

        //Mescla os dados que recebemos na requisição DTO com dados do banco
        Usuario usuario = usuarioConverter.updateUsuario(dto, usuarioEntity);

        //Salva os dados do usuário convertido e pega o retorno e converte para UsuarioDTO
        return usuarioConverter.paraUsuarioDTO(usuarioRepository.save(usuario));
    }

    public EnderecoDTO atualizaEndereco(Long idEndereco, EnderecoDTO enderecoDTO){
        Endereco enderecoEntity = enderecoRepository.findById(idEndereco).orElseThrow(() ->
                new ResourceNotFoundException("Id não encontrado " + idEndereco));

        Endereco endereco = usuarioConverter.updateEndereco(enderecoDTO, enderecoEntity);

        return  usuarioConverter.paraEnderecoDTO(enderecoRepository.save(endereco));
    }

    public TelefoneDTO atualizaTelefone(Long idTelefone, TelefoneDTO telefoneDTO){
        Telefone telefoneEntity = telefoneRepository.findById(idTelefone).orElseThrow(() ->
                new ResourceNotFoundException("Id não encontrado " + idTelefone));

        Telefone telefone = usuarioConverter.updateTelefone(telefoneDTO, telefoneEntity);

        return usuarioConverter.paraTelefoneDTO(telefoneRepository.save(telefone));
    }
}
